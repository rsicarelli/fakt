#!/bin/bash
# Add missing User import to DataCacheTest
sed -i '' '3a\
\
import com.rsicarelli.fakt.samples.singleModule.models.User
' src/commonTest/kotlin/com/rsicarelli/fakt/samples/singleModule/scenarios/3_generics_basic/DataCacheTest.kt

# Add missing Product import to ProductServiceTest
sed -i '' '3a\
\
import com.rsicarelli.fakt.samples.singleModule.models.Product
' src/commonTest/kotlin/com/rsicarelli/fakt/samples/singleModule/scenarios/1_basic/ProductServiceTest.kt

echo "✓ Added missing imports to DataCacheTest and ProductServiceTest"
