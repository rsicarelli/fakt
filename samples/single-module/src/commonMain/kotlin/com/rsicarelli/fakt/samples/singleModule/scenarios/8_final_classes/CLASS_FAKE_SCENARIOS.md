# Class Fake Scenarios

Comprehensive test coverage for class-based fake generation.

## Scenario Categories

### 1. Basic (Simple open/abstract classes)
- **OpenClassNoMethods**: Empty open class (should be ignored - no overridable members)
- **OpenClassSingleMethod**: Open class with one open method
- **AbstractClassSingleAbstract**: Abstract class with one abstract method
- **OpenClassMultipleMethods**: Open class with multiple open methods
- **AbstractClassMultipleAbstract**: Abstract class with multiple abstract methods

### 2. Open Class Variations
- **OpenClassWithProperties**: Open class with open properties (val/var)
- **OpenClassWithReturnTypes**: Open class with various return types (primitives, collections, custom)
- **OpenClassWithDefaultParams**: Open class with methods having default parameters
- **OpenClassWithVarargs**: Open class with vararg parameters
- **OpenClassWithSuspend**: Open class with suspend functions
- **OpenClassGenericMethods**: Open class with generic methods (method-level generics)

### 3. Abstract Class Variations
- **AbstractClassWithProperties**: Abstract class with abstract properties
- **AbstractClassWithReturnTypes**: Abstract class with various return types
- **AbstractClassWithSuspend**: Abstract class with suspend abstract methods
- **AbstractClassGenericMethods**: Abstract class with generic abstract methods
- **AbstractClassWithConstructor**: Abstract class with constructor parameters

### 4. Mixed (Abstract + Open)
- **MixedAbstractAndOpen**: Abstract class with both abstract and open methods
- **MixedPropertiesAndMethods**: Abstract class with abstract properties + open methods
- **MixedSuspendRegular**: Abstract class with suspend abstract + regular open methods
- **MixedGenericMethods**: Abstract class with generic abstract + generic open methods
- **MixedDefaultBehaviors**: Abstract class testing error defaults (abstract) vs super defaults (open)

### 5. Edge Cases
- **ClassWithFinalMethods**: Open class with mix of open and final methods (final ignored)
- **ClassWithPrivateMethods**: Open class with private methods (ignored)
- **ClassWithProtectedMethods**: Open class with protected open methods
- **ClassWithInternalMethods**: Open class with internal open methods
- **NestedClassScenario**: Open class with nested classes (outer class faked)
- **ClassWithCompanionObject**: Open class with companion object (companion ignored)
- **ClassImplementingInterface**: Open class implementing interface (class fake, not interface)
- **ClassWithAnnotations**: Open class with annotated methods
- **ClassWithTypeAliases**: Open class using type aliases in signatures
- **ClassWithNullableTypes**: Open class with nullable parameters and return types

## Priority Levels

**P0 (Critical - Must Work)**:
- OpenClassSingleMethod
- AbstractClassSingleAbstract
- OpenClassMultipleMethods
- AbstractClassMultipleAbstract
- MixedAbstractAndOpen
- ClassWithFinalMethods

**P1 (High - Common Use Cases)**:
- OpenClassWithProperties
- OpenClassWithReturnTypes
- AbstractClassWithProperties
- AbstractClassWithSuspend
- MixedPropertiesAndMethods
- ClassWithNullableTypes

**P2 (Medium - Advanced Features)**:
- OpenClassWithDefaultParams
- OpenClassWithSuspend
- OpenClassGenericMethods
- AbstractClassGenericMethods
- MixedDefaultBehaviors
- ClassWithProtectedMethods

**P3 (Low - Edge Cases)**:
- OpenClassWithVarargs
- AbstractClassWithConstructor
- ClassWithPrivateMethods
- ClassWithInternalMethods
- NestedClassScenario
- ClassWithCompanionObject
- ClassImplementingInterface
- ClassWithAnnotations
- ClassWithTypeAliases

## Implementation Plan

Follow this order for TDD:
1. P0 scenarios first (basic functionality)
2. P1 scenarios (common patterns)
3. P2 scenarios (advanced features)
4. P3 scenarios (edge cases - may skip some)

Each scenario:
1. Create class in appropriate folder
2. Document the scenario with KDoc
3. Create corresponding test with GIVEN-WHEN-THEN pattern
4. Verify compilation and test execution
5. Move to next scenario

## Expected Behavior Patterns

### Abstract Methods
```kotlin
// Default: Error (must configure)
private var methodBehavior: (Params) -> Return = { ... -> error("Configure method behavior") }
```

### Open Methods
```kotlin
// Default: Super call (optional override)
private var methodBehavior: (Params) -> Return = { params -> super.method(params) }
```

### Final Methods
```kotlin
// Ignored - not included in fake (use original implementation)
```

## Test Coverage Goals

- **Minimum**: 30 scenarios (10 per P0/P1)
- **Target**: 40 scenarios (covering P0-P2)
- **Stretch**: 50+ scenarios (full P0-P3 coverage)
