// Copyright (C) 2025 Rodrigo Sicarelli
// SPDX-License-Identifier: Apache-2.0
package com.rsicarelli.fakt.compiler.runner

import com.rsicarelli.fakt.codegen.analysis.FakeDeclaration
import com.rsicarelli.fakt.compiler.core.context.FaktSharedContext
import com.rsicarelli.fakt.compiler.core.types.createTypeResolution
import com.rsicarelli.fakt.compiler.ir.analysis.toFakeClass
import com.rsicarelli.fakt.compiler.ir.analysis.toFakeInterface
import com.rsicarelli.fakt.compiler.ir.transform.FirToIrTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.util.packageFqName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

/**
 * IR extension that translates Fakt's FIR-validated metadata into [FakeDeclaration]s and emits each
 * one to the supplied callback — without writing any files.
 *
 * Intended for use by the headless analysis runner in `:compiler-runner`. The production
 * [com.rsicarelli.fakt.compiler.ir.generation.UnifiedFaktIrGenerationExtension] writes generated
 * `.kt` files as a side effect of `compileKotlin*`; the cache-correct architecture (issue #79)
 * needs that translation step to be runnable from a Gradle `@CacheableTask` so the file write can
 * happen against the task's `@OutputDirectory`. This extension provides exactly that translation
 * step in callback form, leaving file emission to the caller.
 *
 * @param sharedContext FIR/IR shared state; the same instance must be passed to
 *   [com.rsicarelli.fakt.compiler.fir.FaktFirExtensionRegistrar] so that validated metadata
 *   produced by FIR checkers reaches this extension via [FaktSharedContext.metadataStorage].
 * @param onDeclaration Invoked once per validated `@Fake` declaration discovered in the module.
 */
public class CaptureIrGenerationExtension(
    private val sharedContext: FaktSharedContext,
    private val onDeclaration: (FakeDeclaration) -> Unit,
) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val typeResolver = createTypeResolution()
        val transformer = FirToIrTransformer(typeResolution = typeResolver)
        val irClassMap = buildIrClassMap(moduleFragment)

        sharedContext.metadataStorage.getAllInterfaces().forEach { firInterface ->
            val irClass = irClassMap[firInterface.classId] ?: return@forEach
            val metadata = transformer.transform(firInterface, irClass)
            onDeclaration(
                metadata.toFakeInterface(
                    typeResolver = typeResolver,
                    enableCallHistoryDefault = sharedContext.options.enableCallHistoryDefault,
                    enableMutableFakesDefault = sharedContext.options.enableMutableFakesDefault,
                )
            )
        }

        sharedContext.metadataStorage.getAllClasses().forEach { firClass ->
            val irClass = irClassMap[firClass.classId] ?: return@forEach
            val metadata = transformer.transformClass(firClass, irClass)
            onDeclaration(
                metadata.toFakeClass(
                    typeResolver = typeResolver,
                    enableCallHistoryDefault = sharedContext.options.enableCallHistoryDefault,
                    enableMutableFakesDefault = sharedContext.options.enableMutableFakesDefault,
                )
            )
        }
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun buildIrClassMap(moduleFragment: IrModuleFragment): Map<ClassId, IrClass> {
        val map = mutableMapOf<ClassId, IrClass>()
        moduleFragment.files.forEach { file ->
            file.declarations.filterIsInstance<IrClass>().forEach { irClass ->
                indexClassRecursively(
                    irClass = irClass,
                    packageFqName = irClass.packageFqName ?: FqName.ROOT,
                    relativeNameSegments = listOf(irClass.name.asString()),
                    map = map,
                )
            }
        }
        return map
    }

    @OptIn(UnsafeDuringIrConstructionAPI::class)
    private fun indexClassRecursively(
        irClass: IrClass,
        packageFqName: FqName,
        relativeNameSegments: List<String>,
        map: MutableMap<ClassId, IrClass>,
    ) {
        val classId =
            ClassId(
                packageFqName = packageFqName,
                relativeClassName = FqName(relativeNameSegments.joinToString(".")),
                isLocal = false,
            )
        map[classId] = irClass
        irClass.declarations.filterIsInstance<IrClass>().forEach { nested ->
            indexClassRecursively(
                irClass = nested,
                packageFqName = packageFqName,
                relativeNameSegments = relativeNameSegments + nested.name.asString(),
                map = map,
            )
        }
    }
}
