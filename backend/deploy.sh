#!/bin/bash

set -e

echo "🚀 Deploying Wahapedia to 1GB VPS"

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo "❌ Docker not found. Installing..."
    # Install Docker based on distro
    if command -v apt-get &> /dev/null; then
        curl -fsSL https://get.docker.com -o get-docker.sh
        sudo sh get-docker.sh
        sudo usermod -aG docker $USER
    elif command -v yum &> /dev/null; then
        sudo yum install -y docker
        sudo systemctl start docker
        sudo usermod -aG docker $USER
    else
        echo "❌ Unsupported package manager. Please install Docker manually."
        exit 1
    fi
fi

# Check if docker-compose is installed
if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose not found. Installing..."
    sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
fi

echo "📁 Creating required directories..."
mkdir -p data db logs

echo "⚙️  Optimizing system for 1GB RAM..."
# Add sysctl optimizations for low memory
echo 'vm.swappiness=10' | sudo tee -a /etc/sysctl.conf
echo 'vm.vfs_cache_pressure=50' | sudo tee -a /etc/sysctl.conf

echo "🐳 Building and starting containers..."

# Use native image by default
echo "🔥 Building with GraalVM Native Image (recommended for 1GB VPS)..."
docker-compose up -d app-native

echo ""
echo "✅ Deployment complete!"
echo ""
echo "📊 Container status:"
docker-compose ps

echo ""
echo "💾 Resource usage:"
echo "Memory: $(free -h | awk '/^Mem:/ {print $3 "/" $2}')"
echo "Disk: $(df -h . | awk 'NR==2 {print $3 " / " $2 " (" $5 " used)"}')"

echo ""
echo "🧪 Test deployment:"
echo "curl http://localhost:8080/health"
echo ""
echo "📝 Monitor logs:"
echo "docker-compose logs -f app-native"
echo ""
echo "💡 If native image fails, switch to JVM fallback:"
echo "docker-compose down && docker-compose up -d app-jvm"