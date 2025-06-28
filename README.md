BIMserver
=========

The Building Information Model server (short: BIMserver) enables you to store and manage the information of a construction (or other building related) project. Data is stored in the open data standard IFC. The BIMserver is not a fileserver, but it uses a model-driven architecture approach. This means that IFC data is stored as objects. You could see BIMserver as an IFC database, with special extra features like model checking, versioning, project structures, merging, etc. The main advantage of this approach is the ability to query, merge and filter the BIM-model and generate IFC output (i.e. files) on the fly.

Thanks to its multi-user support, multiple people can work on their own part of the dataset, while the complete dataset is updated on the fly. Other users can get notifications when the model (or a part of it) is updated. 

BIMserver is built for developers. We've got a great wiki on https://github.com/opensourceBIM/BIMserver/wiki and are very active supporting developers on https://github.com/opensourceBIM/BIMserver/issues 

## Building from Source

### Prerequisites

To build BIMserver from source, you need:

- **Java Development Kit (JDK) 8 or higher** - The project is configured for Java 8 compatibility but works with newer versions
- **Apache Maven 3.x** - For dependency management and building
- **Git** - To clone the repository

### Building the Application

1. **Clone the repository:**
   ```bash
   git clone https://github.com/opensourceBIM/BIMserver.git
   cd BIMserver
   ```

2. **Build the complete project:**
   ```bash
   mvn clean package
   ```

   To skip tests during build (faster for development):
   ```bash
   mvn clean package -DskipTests
   ```

3. **Build specific artifacts:**
   - WAR file only: `mvn clean package -pl BimServerWar -am -DskipTests`
   - Standalone JAR only: `mvn clean package -pl BimServerJar -am -DskipTests`

### Build Artifacts

The build process creates two main deployment artifacts:

- **WAR file** (`BimServerWar/target/bimserverwar-{version}.war`): 
  - For deployment to servlet containers like Tomcat, Jetty, or application servers
  - Requires external servlet container setup

- **Standalone JAR** (`BimServerJar/target/starter.jar`):
  - Self-contained executable with embedded Jetty server
  - Ideal for evaluation, development, and simple deployments
  - No external dependencies required

### Running BIMserver

#### Option 1: Standalone JAR (Recommended for testing)
```bash
java -jar BimServerJar/target/starter.jar
```

The server will start on `http://localhost:8080` by default.

**Note:** The standalone JAR includes a GUI component for setup. For headless servers, you may need to add `-Djava.awt.headless=true` to the Java command or use the direct server class:

```bash
# Alternative headless startup (server runs on port 8082)
java -Djava.awt.headless=true -cp BimServerJar/target/starter.jar org.bimserver.JarBimServer
```

#### Option 2: WAR Deployment
Deploy the WAR file (`BimServerWar/target/bimserverwar-{version}.war`) to your preferred servlet container following the container's deployment procedures.

### Development

#### Running Tests
```bash
mvn test -Ptest
```
*Note: Integration tests require a running BIMserver instance.*

## Automated Deployment (CI/CD)

This repository includes GitHub Actions workflow for automated building and deployment:

- **Automatic builds** triggered on push to main/master branch
- **WAR file deployment** to `/opt/tomcat9/webapps/ROOT.war`
- **Tomcat service management** with automatic restart

### Quick Setup for Deployment

1. **Prepare your server:**
   ```bash
   # Run the setup script on your target server
   curl -sSL https://raw.githubusercontent.com/ikozyrev3/BIMserver/main/.github/setup-server.sh | bash
   ```

2. **Configure GitHub secrets** (see [.github/DEPLOYMENT.md](.github/DEPLOYMENT.md) for details):
   - `DEPLOY_HOST`: Your server IP/hostname
   - `DEPLOY_USER`: SSH username
   - `DEPLOY_KEY`: SSH private key

3. **Push to main branch** to trigger automatic deployment

For detailed setup instructions, see [Deployment Documentation](.github/DEPLOYMENT.md).

#### IDE Setup
The project uses standard Maven structure and can be imported into any IDE that supports Maven projects. All modules are organized under the root directory.

#### Build Modules
- `Bdb`: Berkeley database integration
- `BimServer`: Core server implementation
- `BimServerClientLib`: Client library for connecting to BIMserver
- `PluginBase`: Plugin architecture foundation
- `Shared`: Shared utilities and interfaces
- `BimServerWar`: WAR packaging module
- `BimServerJar`: Standalone JAR packaging module
- `Tests`: Test suite

## Automated Deployment (CI/CD)

This repository includes GitHub Actions workflow for automated building and deployment:

- **Automatic builds** triggered on push to main/master branch
- **WAR file deployment** to `/opt/tomcat9/webapps/ROOT.war`
- **Tomcat service management** with automatic restart

### Quick Setup for Deployment

1. **Prepare your server:**
   ```bash
   # Run the setup script on your target server
   curl -sSL https://raw.githubusercontent.com/ikozyrev3/BIMserver/main/.github/setup-server.sh | bash
   ```

2. **Configure GitHub secrets** (see [.github/DEPLOYMENT.md](.github/DEPLOYMENT.md) for details):
   - `DEPLOY_HOST`: Your server IP/hostname
   - `DEPLOY_USER`: SSH username
   - `DEPLOY_KEY`: SSH private key

3. **Push to main branch** to trigger automatic deployment

For detailed setup instructions, see [Deployment Documentation](.github/DEPLOYMENT.md).

## License and Copyright

(C) Copyright by the contributers / BIMserver.org

Licence: GNU Affero General Public License, version 3 (see http://www.gnu.org/licenses/agpl-3.0.html)
Beware: this project makes intensive use of several other projects with different licenses. Some plugins and libraries are published under a different license.
