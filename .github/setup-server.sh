#!/bin/bash

# BIMserver Deployment Server Setup Script
# This script prepares a Ubuntu/Debian server for BIMserver deployment

set -e

echo "=== BIMserver Deployment Server Setup ==="
echo "This script will install and configure Tomcat 9 for BIMserver deployment"
echo

# Check if running as root
if [[ $EUID -eq 0 ]]; then
   echo "This script should not be run as root. Please run as a regular user with sudo privileges."
   exit 1
fi

# Update system
echo "Updating system packages..."
sudo apt update

# Install Java 8 if not present
if ! java -version 2>&1 | grep -q "1.8\|11\|17"; then
    echo "Installing OpenJDK 8..."
    sudo apt install -y openjdk-8-jdk
else
    echo "Java is already installed:"
    java -version
fi

# Install Tomcat 9
echo "Installing Tomcat 9..."
sudo apt install -y tomcat9 tomcat9-admin

# Create tomcat user if it doesn't exist (usually created by package)
if ! id "tomcat" &>/dev/null; then
    echo "Creating tomcat user..."
    sudo useradd -r -s /bin/false tomcat
fi

# Set proper ownership for Tomcat directories
echo "Setting up Tomcat directories..."
sudo chown -R tomcat:tomcat /opt/tomcat9/
sudo chown -R tomcat:tomcat /var/lib/tomcat9/
sudo chown -R tomcat:tomcat /var/log/tomcat9/

# Enable and start Tomcat
echo "Enabling and starting Tomcat service..."
sudo systemctl enable tomcat9
sudo systemctl start tomcat9

# Wait a bit for Tomcat to start
sleep 5

# Check Tomcat status
if sudo systemctl is-active --quiet tomcat9; then
    echo "✓ Tomcat 9 is running successfully"
else
    echo "✗ Tomcat 9 failed to start. Check logs with: sudo journalctl -u tomcat9"
    exit 1
fi

# Get the current user for sudoers setup
CURRENT_USER=$(whoami)

# Setup sudoers for deployment
SUDOERS_FILE="/etc/sudoers.d/bimserver-deploy"
echo "Setting up sudo permissions for deployment..."
sudo tee $SUDOERS_FILE > /dev/null <<EOF
# BIMserver deployment permissions for user: $CURRENT_USER
$CURRENT_USER ALL=(ALL) NOPASSWD: /bin/mv /tmp/ROOT.war /opt/tomcat9/webapps/ROOT.war
$CURRENT_USER ALL=(ALL) NOPASSWD: /bin/chown tomcat:tomcat /opt/tomcat9/webapps/ROOT.war
$CURRENT_USER ALL=(ALL) NOPASSWD: /bin/systemctl restart tomcat9
$CURRENT_USER ALL=(ALL) NOPASSWD: /bin/systemctl stop tomcat9
$CURRENT_USER ALL=(ALL) NOPASSWD: /bin/systemctl start tomcat9
$CURRENT_USER ALL=(ALL) NOPASSWD: /bin/systemctl status tomcat9
EOF

echo "✓ Sudo permissions configured for user: $CURRENT_USER"

# Configure firewall if ufw is available
if command -v ufw &> /dev/null; then
    echo "Configuring firewall for Tomcat (port 8080)..."
    sudo ufw allow 8080/tcp
    echo "✓ Firewall configured to allow port 8080"
fi

# Display Tomcat information
echo
echo "=== Setup Complete ==="
echo "Tomcat 9 is installed and running"
echo "Default port: 8080"
echo "Webapps directory: /opt/tomcat9/webapps/"
echo "Logs directory: /var/log/tomcat9/"
echo

# Test Tomcat
TOMCAT_PORT=8080
if curl -s http://localhost:$TOMCAT_PORT > /dev/null; then
    echo "✓ Tomcat is responding on port $TOMCAT_PORT"
    echo "Access your server at: http://$(hostname -I | awk '{print $1}'):$TOMCAT_PORT"
else
    echo "✗ Tomcat is not responding on port $TOMCAT_PORT"
    echo "Check logs: sudo journalctl -u tomcat9 -f"
fi

echo
echo "=== Next Steps ==="
echo "1. Configure your GitHub repository secrets:"
echo "   - DEPLOY_HOST: $(hostname -I | awk '{print $1}')"
echo "   - DEPLOY_USER: $CURRENT_USER"
echo "   - DEPLOY_KEY: Your SSH private key content"
echo
echo "2. Generate SSH key pair if you haven't already:"
echo "   ssh-keygen -t rsa -b 4096 -f ~/.ssh/bimserver_deploy"
echo
echo "3. Copy your public key to this server:"
echo "   ssh-copy-id -i ~/.ssh/bimserver_deploy.pub $CURRENT_USER@$(hostname -I | awk '{print $1}')"
echo
echo "4. Push code to main/master branch to trigger deployment"
echo
echo "For more details, see .github/DEPLOYMENT.md in your repository"