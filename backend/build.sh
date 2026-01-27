#!/bin/bash

set -e

echo "🚀 Building Wahapedia for Small Footprint Deployment"

# Create necessary directories
mkdir -p data db logs

echo "📦 Building with sbt..."
sbt clean universal:packageBin

echo "🔥 Building JAR assembly for Docker..."
sbt assembly

echo "🐳 Building Docker images..."

# Build JVM image
echo "Building JVM Docker image (optimized for 1GB VPS)..."
docker build -f Dockerfile.jvm -t wahapedia-jvm .

# Check for native-image tool
echo "🔍 Checking for GraalVM native-image..."
if command -v native-image &> /dev/null; then
    echo "✅ GraalVM native-image found"
    HAS_NATIVE=true
else
    echo "⚠️  GraalVM native-image not found - using JVM deployment"
    echo "💡 To enable native image: Install GraalVM or add native-image to PATH"
    HAS_NATIVE=false
fi

# Try native image if available
if [ "$HAS_NATIVE" = true ]; then
    echo "🚀 Building GraalVM native image..."
    if docker build -f Dockerfile.native -t wahapedia-native . 2>/dev/null; then
        echo "✅ Native image built successfully!"
        NATIVE_BUILT=true
    else
        echo "⚠️  Native image build failed"
        NATIVE_BUILT=false
    fi
else
    NATIVE_BUILT=false
fi

echo "✅ Build complete!"
echo ""
if [ "$NATIVE_BUILT" = true ]; then
    echo "📊 Image sizes:"
    echo "Native: $(docker images wahapedia-native --format "table {{.Repository}}\t{{.Size}}")"
    echo "JVM: $(docker images wahapedia-jvm --format "table {{.Repository}}\t{{.Size}}")"
    echo ""
    echo "🎯 To run:"
    echo "Native (recommended): docker-compose up app-native"
    echo "JVM fallback:      docker-compose up app-jvm"
else
    echo "📊 JVM Image size:"
    echo "JVM: $(docker images wahapedia-jvm --format "table {{.Repository}}\t{{.Size}}")"
    echo ""
    echo "🎯 To run:"
    echo "JVM: docker-compose up app-jvm"
fi

echo ""
echo "🧪 To test:"
if [ "$NATIVE_BUILT" = true ]; then
    echo "Native: curl http://localhost:8080/health"
fi
echo "JVM:    curl http://localhost:8081/health"

echo ""
echo "💾 Database will be stored in ./db/"
echo "📁 Data files in ./data/"
echo "📝 Logs in ./logs/"