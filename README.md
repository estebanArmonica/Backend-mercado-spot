## Sistema de Mercado Spot - ETL
Sistema ETL (Extract, Transform, Load) desarrollado en Java para procesar archivos Excel y cargar los datos estructurados en una base de datos Supabase (PostgreSQL).

## 📋 Tabla de Contenidos

- [Características](#-características)
- [Arquitectura del Sistema](#-arquitectura-del-sistema)
- [Tecnologías](#-tecnologías)
- [Requisitos Previos](#-requisitos-previos)
- [Instalación](#-instalación)
- [Estructura del Proyecto](#-estructura-del-proyecto)
- [Flujo de Trabajo](#-flujo-de-trabajo)
- [Uso](#-uso)
- [Manejo de Errores](#-manejo-de-errores)
- [Ejecución](#-ejecución)
- [Logging y Monitoreo](#-logging-y-monitoreo)
- [Pruebas](#-pruebas)
- [Documentación API](#-documentación-api)
- [Despliegue](#-despliegue)
- [Contribución](#-contribución)
- [Licencia](#-licencia)

## ✨ Características

- **Extracción**: Lectura eficiente de archivos Excel (.xlsx, .xls) usando Apache POI
- **Transformación**: Validación, limpieza y mapeo de datos según reglas de negocio
- **Carga**: Inserción masiva optimizada un Supabase usando JDBC 
- **Gestión de Errores**: Sistema robusto con reintentos y logging detallado
- **Monitoreo**: Garbage Collector monitoring y estadísticas de memoria
- **Documentación**: Swagger UI para visualización de documentación por APIs
- **Rendimiento**: Procesamiento por lotes para grandes volúmenes de datos
- **Configurable**: Parámetro ajustable vía archivos de propiedades
- **Seguro**: Uso de variables de entorno para credenciales sensibles 

## 🏗️ Arquitectura del Sistema

```text
[Archivo Excel] → [Extractor] → [Transformer] → [Loader] → [Supabase]
                       ↓              ↓             ↓
                  [Validador]   [Mapper]    [Batch Processor]
```
# Componentes Principales

1. **Extractor**: Lee y parsea archivos Excel
2. **Transformer**: Valida y transforma los datos
3. **Loader**: Gestiona la conexión y carga en Supabase
4. **ETL Orchestrator**: Coordina todo el flujo

## 🛠️ Tecnologías Utilizada

| Tecnología          | Versión | Propósito               |
|---------------------|---------|-------------------------|
| **Java**            | 17+     | Lenguaje Base           |
| **Apache POI**      | 5.2.3   | Lectura de Excel        |
| **PostgreSQL JDBC** | 42.6.0  | Conexión a Supabase     |
| **SLF4J + Logback** | 2.0.9   | Logging                 |
| **Jackson**         | 2.15.2  | Manejo de JSON          |
| **JUnit**           | 5.10.0  | Pruebas Unitarias       |
| **Maven**           | 3.9.0   | Gestión de dependencias |

## 📦 Requisitos Previos
- **Java 17** o superior
- **Maven 3.9** o superior
- **Cuenta en Supabase** (proyecto creado) 
- **Acceso a la base de datos** (URL, puerto, usuario, contraseña)
- **Archivo Excel** con formato definido

## 🔧 Instalación y Configuración

1. Clona el repositorio

```bash
    git clone <repo>
    cd proyecto_java
```

2. configura las variables de entorno
```bash
    # datos de postgresql en produccion
    SUPERBASE_URL=url
    SUPERBASE_USER=user
    SUPERBASE_PASS=pass

    # Variables del JWT
    JWT_FIRMA=firma-de-512
    JWT_EXPIRATION=numeros

    LOGGING_FILE_NAME=carpeta de logs
    SERVER_PORT_PROD=puerto donde escucha
```

3. configura la aplicación
Edita la ruta de `src/main/resources/application.properties`

```bash
    spring.application.name=Mercado_Spot

    # definimos un puerto y una URL para entrar con swagger
    server.port=${SERVER_PORT_PROD}

    # agregamos los datos para la conexion
    spring.datasource.url=${SUPERBASE_URL}
    spring.datasource.username=${SUPERBASE_USER}
    spring.datasource.password=${SUPERBASE_PASS}
```

>[!NOTE]
> Spring Boot te deja crear más de un `application.properties` en caso de querer realizar uno para pruebas
> de testing y una para producción.
>
> Se define dejando uno cómo archivo principal para levantar los demás dependiendo el `profiles`

```bash
    # se puede dejar así y llamar al application.properties correspondientes que quieras levantar
    spring.profiles.active=dev # dev/prod/test
```
4. Compilamos el proyecto con maven

```bash
    mvn clean install
```

## 📁 Estructura del Proyecto

```text
java-etl-excel-supabase/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/safiraenergia/mercadospot/
│   │   │       ├── config/
│   │   │       │   ├── components/
│   │   │       │   |      └── DataBaseInitializer.java
│   │   │       │   ├── AsyncConfig.java
│   │   │       │   ├── CorsConfig.java
│   │   │       │   ├── GarbageCollectorMonitoringConfig.java
│   │   │       │   ├── RestApiConfig.java
│   │   │       │   └── SwaggerConfig.java
│   │   │       ├── Extract/
│   │   │       │   ├── reports/
│   │   │       │   |      └── DashboardReportsController.java
│   │   │       │   ├── AuthController.java
│   │   │       │   ├── EntidadController.java
│   │   │       │   ├── EstadoController.java
│   │   │       │   ├── EtlController.java
│   │   │       │   ├── FacturaController.java
│   │   │       │   ├── GlosaController.java
│   │   │       │   ├── PeriodoController.java
│   │   │       │   ├── TipoEntidadController.java
│   │   │       │   └── UsuarioController.java
│   │   │       ├── dto/
│   │   │       │   ├── auth/
│   │   │       │   |      ├── LoginRequest.java
│   │   │       │   |      ├── LoginResponse.java
│   │   │       │   |      └── RegisterRequest.java
│   │   │       │   ├── entidad/
│   │   │       │   |      └── entidadDTO.java
│   │   │       │   ├── estado/
│   │   │       │   |      └── estadoDTO.java
│   │   │       │   ├── etl/
│   │   │       │   |      ├── ETLProgressDTO.java
│   │   │       │   |      └── ETLResultDTO.java
│   │   │       │   ├── factura/
│   │   │       │   |      ├── FacturaDTO.java
│   │   │       │   |      ├── FacturaFilterDTO.java
│   │   │       │   |      └── FacturaResponseDTO.java
│   │   │       │   ├── glosa/
│   │   │       │   |      └── GlosaDTO.java
│   │   │       │   ├── periodo/
│   │   │       │   |      └── PeriodoDTO.java
│   │   │       │   ├── tipoentidad/
│   │   │       │   |      └── TipoEntidadDTO.java
│   │   │       │   ├── user/
│   │   │       │   |      ├── ChangePasswordRequest.java
│   │   │       │   |      └── UpdateProfileRequest.java
│   │   │       ├── enums/
│   │   │       │   └── ETLStatus.java
│   │   │       ├── etl/
│   │   │       │   ├── extractor/
│   │   │       │   |      ├── utils/
│   │   │       │   |      |      └── ExcelDataExtractor.java
│   │   │       │   |      ├── DataExtractor.java
│   │   │       │   |      └── ExtractorFactory.java
│   │   │       │   ├── loader/
│   │   │       │   |      ├── service/
│   │   │       │   |      |      └── ETLTransactionService.java
│   │   │       │   |      ├── DataLoader.java
│   │   │       │   |      └── LoadResult.java
│   │   │       │   ├── transformer/
│   │   │       │   |      ├── validation/
│   │   │       │   |      |      ├── ValidationChain.java
│   │   │       │   |      |      └── Validator.java
│   │   │       │   |      └── DataTransformer.java
│   │   │       ├── exceptions/
│   │   │       │   ├── EntityNotFoundException.java
│   │   │       │   ├── ETLException.java
│   │   │       │   ├── ExtractionException.java
│   │   │       │   ├── GlobalException.java
│   │   │       │   ├── TransformationException.java
│   │   │       │   └── ValidationException.java
│   │   │       ├── models/
│   │   │       │   ├── Entidad.java
│   │   │       │   ├── Estado.java
│   │   │       │   ├── Factura.java
│   │   │       │   ├── Glosa.java
│   │   │       │   ├── Periodo.java
│   │   │       │   ├── RefreshToken.java
│   │   │       │   ├── Rol.java
│   │   │       │   ├── TipoEntidad.java
│   │   │       │   └── Usuario.java
│   │   │       ├── repository/
│   │   │       │   ├── IEntidadRepository.java
│   │   │       │   ├── IEstadoRepository.java
│   │   │       │   ├── IFacturaRepository.java
│   │   │       │   ├── IGlosaRepository.java
│   │   │       │   ├── IPeriodoRepository.java
│   │   │       │   ├── IRefreshTokenRepository.java
│   │   │       │   ├── IRolRepository.java
│   │   │       │   ├── ITipoEntidadRepository.java
│   │   │       │   └── IUsuarioRepository.java
│   │   │       ├── security/
│   │   │       │   ├── CustomUserDetailsService.java
│   │   │       │   ├── JwtAuthenticationEntryPoint.java
│   │   │       │   ├── JwtAuthenticationFilter.java
│   │   │       │   ├── JwtGenerador.java
│   │   │       │   ├── RefreshTokenService.java
│   │   │       │   ├── SecurityConfig.java
│   │   │       │   ├── SecurityConstants.java
│   │   │       │   └── SqlInjectionFilter.java
│   │   │       ├── services/
│   │   │       │   ├── auth/
│   │   │       │   |      ├── impl/
│   │   │       │   |      |     └── AuthServiceImpl.java
│   │   │       │   |      └── IAuthService.java
│   │   │       │   ├── core/
│   │   │       │   |      ├── impl/
│   │   │       │   |      |     └── GenericServiceImpl.java
│   │   │       │   |      ├── utils/
│   │   │       │   |      |     └── GenericServiceImpl.java
│   │   │       │   |      └── ICrudService.java
│   │   │       │   ├── entidad/
│   │   │       │   |      ├── impl/
│   │   │       │   |      |     └── EntidadServiceImpl.java
│   │   │       │   |      └── IEntidadService.java
│   │   │       │   ├── estado/
│   │   │       │   |      ├── impl/
│   │   │       │   |      |     └── EstadoServiceImpl.java
│   │   │       │   |      └── IEstadoService.java
│   │   │       │   ├── etl/
│   │   │       │   |      ├── impl/
│   │   │       │   |      |     └── ETLProcessorServiceImpl.java
│   │   │       │   |      └── IETLProcessorService.java
│   │   │       │   ├── factura/
│   │   │       │   |      ├── impl/
│   │   │       │   |      |     └── FacturaServiceImpl.java
│   │   │       │   |      └── IFacturaService.java
│   │   │       │   ├── glosa/
│   │   │       │   |      ├── impl/
│   │   │       │   |      |     └── GlosaServiceImpl.java
│   │   │       │   |      └── IGlosaService.java
│   │   │       │   ├── periodo/
│   │   │       │   |      ├── impl/
│   │   │       │   |      |     └── PeriodoServiceImpl.java
│   │   │       │   |      └── IPeriodoService.java
│   │   │       │   ├── tipoentidad/
│   │   │       │   |      ├── impl/
│   │   │       │   |      |     └── TipoEntidadServiceImpl.java
│   │   │       │   |      └── ITipoEntidadService.java
│   │   │       │   ├── user/
│   │   │       │   |      ├── impl/
│   │   │       │   |      |     └── UserServiceImpl.java
│   │   │       │   |      └── IUserService.java
│   │   │       ├── specification/
│   │   │       │   └── FacturaSpecification.java
│   │   │       ├── utils/
│   │   │       │   └── ETLLogger.java
│   │   │       └── BackendEtlMercadoSpotApplication.java
│   │   └── resources/
│   │       ├── application-dev.properties
│   │       ├── application-prod.properties
│   │       └── application.properties
│   └── test/
│       └── java/
│       |   └── com/safiraenergia/mercadospot/
│       │   │   ├── integrations/
│       │   │   │   └── SqlInjectionFilterTest.java
│               └── BackendEtlMercadoSpotApplicationTests.java
├── pom.xml
├── README.md
├── Jenkinsfile
├── LICENSE
├── NOTICE
└── .env

```

## 🔄 Flujo de Trabajo

1. Extracción (Extract)

```java
    ExcelExtractor extractor = new ExcelExtractor();
    List<Map<String, Object>> rawData = extractor.extract("archivo.xlsx");
```

2. Transformación (Transform)

```java
    DataTransformer transformer = new DataTransformer();
    List<Entity> transformedData = transformer.transform(rawData);
```

3. Carga (Load)

```java
    SupabaseLoader loader = new SupabaseLoader();
    loader.load(transformedData);
```

## 🚀 Uso

Ejecuta desde la línea de comandos (CMD, BASH, ZSH, POWERSHELL)

```bash
    # para levantar el proeycto solo se usa maven

    # limpiamos el target
    mvn clean 

    # compilamos el target
    mvn compile

    # levantamos el proyecto
    mvn spring-boot:run
```

## 🛡️ Manejo de Errores

| Error                    | Causa                  | Solución                          |
|--------------------------|------------------------|-----------------------------------|
| `FileNotFoundException`  | Archivo no encontrado  | Verificar ruta y permisos         |
| `SQLException`           | Error de conexión a DB | Verificar credenciales y firewall |
| `DataValidationException`| Datos inválidos        | Revisar formato del excel         |
| `BatchTimeoutException`  | Timeout en batch       | Reducir tamaño de lote            |

# Sistema de Reintentos

```java
    @Retryable(
        value = {SQLException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000)
    )
```

## 📝 Logging y Monitoreo

# Niveles de Log
- **INFO**: Operaciones principales y resumen
- **DEBUG**: Procesamiento detallado
- **ERROR**: Fallos críticos
- **WARN**: Advertencias no criticas

# Ejemplo de Logs

```text
2024-01-15 10:30:15 INFO  - Iniciando proceso ETL
2024-01-15 10:30:16 INFO  - Archivo leído: datos.xlsx (1500 filas)
2024-01-15 10:30:18 INFO  - Transformados: 1498 registros válidos
2024-01-15 10:30:20 INFO  - Cargados: 1498 registros en Supabase
2024-01-15 10:30:20 INFO  - Tiempo total: 5.2 segundos

```

## 🧪 Pruebas
Para realizar las pruebas unitarias, ejecute el comando

```bash
    mvn test
```

# Ejemplo de prueba unitaria

```java
    @Test
    void testExcelExtractor_ValidFile() {
        ExcelExtractor extractor = new ExcelExtractor();
        List<Map<String, Object>> data = extractor.extract("test.xlsx");
        assertNotNull(data);
        assertTrue(data.size() > 0);
    }
```

## 🚀 Documentación API

La documentación de la API está accesible por Swagger tanto su modo dev cómo la de prod.

> [!TIP]
> Puedes acceder a la documentación automática en:
>
> - Swagger UI: `http://localhost:8083/swagger-ui.html`
>
> Esté es solo de la manera local.

| Método    | Endpoint                                   | Descripción                                     | Roles Permitidos   |
|-----------|--------------------------------------------|-------------------------------------------------|--------------------|
| **POST**  | `/api/v1/auth/login`                       | Login del usuario                               | Todos              |
| **POST**  | `/api/v1/auth/logout`                      | Logout del usuario con eliminación de token     | Todos              |
| **POST**  | `/api/v1/etl/upload`                       | Realiza cargas masivas de archivos ETL          | ADMIN, BACK_USER   |
| **GET**   | `/api/v1/etl/progress/{jobId}`             | Consulta el progreso de un trabajo ETL          | ADMIN, BACK_USER   |
| **POST**  | `/api/v1/etl/cancel/{jobId}`               | Cancela un trabajo ETL en progreso              | ADMIN              |
| **PUT**   | `/api/v1/usuarios/updated-profile`         | Actualiza el perfil del usuario autenticado     | ADMIN, BACK_USER   |
| **PUT**   | `/api/v1/usuarios/change-password`         | Cambia la contraseña del usuario autenticado    | ADMIN, BACK_USER   |
| **GET**   | `/api/v1/usuarios/me`                      | Obtiene la información del usuario autenticado  | ADMIN, BACK_USER   |
| **GET**   | `/api/v1/entidad/list-all`                 | Lista todas las entidades                       | ADMIN, BACK_USER   |
| **GET**   | `/api/v1/entidad/list-entidad/{id}`        | Busca una entidad por su ID                     | ADMIN, BACK_USER   |
| **GET**   | `/api/v1/entidad/rut/{rut}`                | Busca una entidad por su RUT                    | ADMIN, BACK_USER   |
| **GET**   | `/api/v1/entidad/deudores`                 | Lista todas las entidades deudoras              | ADMIN, BACK_USER   |
| **GET**   | `/api/v1/entidad/acreedores`               | Lista todas las entidades acreedoras            | ADMIN, BACK_USER   |
| **POST**  | `/api/v1/entidad/create-new-entidad`       | Crea una nueva entidad                          | ADMIN              |
| **PUT**   | `/api/v1/entidad/update-entidad/{id}`      | Actualiza una entidad existente                 | ADMIN              |
| **DELETE**| `/api/v1/entidad/delete-entidad/{id}`      | Elimina una entidad                             | ADMIN              |
| **GET**   | `/api/v1/estados/list-all`                 | Lista todos los estados                         | ADMIN, BACK_USER   |
| **GET**   | `/api/v1/estados/list-estado/{id}`         | Busca un estado por su ID                       | ADMIN, BACK_USER   |
| **GET**   | `/api/v1/estados/descripcion/{descripcion}`| Busca estados por su descripción                | ADMIN, BACK_USER   |
| **POST**  | `/api/v1/estados/create-new-estado`        | Crea un nuevo estado                            | ADMIN              |
| **PUT**   | `/api/v1/estados/update-estado/{id}`       | Actualiza un estado existente                   | ADMIN              |
| **DELETE**| `/api/v1/estados/delete-estado/{id}`       | Elimina un estado                               | ADMIN              |
| **GET**   | `/api/v1/glosa/list-all`                   | Lista todas las glosas                          | ADMIN, BACK_USER   |
| **GET**   | `/api/v1/glosa/list-glosa/{id}`            | Busca una glosa por su ID                       | ADMIN, BACK_USER   |
| **GET**   | `/api/v1/glosa/search`                     | Busca glosas por criterios específicos          | ADMIN, BACK_USER   |
| **POST**  | `/api/v1/glosa/create-new-glosa`           | Crea una nueva glosa                            | ADMIN              |
| **PUT**   | `/api/v1/glosa/update-glosa/{id}`          | Actualiza una glosa existente                   | ADMIN              |
| **DELETE**| `/api/v1/glosa/delete-glosa/{id}`          | Elimina una glosa                               | ADMIN              |
| **GET**   | `/api/v1/factura/list-all`                 | Lista todas las facturas                        | ADMIN, BACK_USER   |
| **GET**   | `/api/v1/factura/list-factura/{id}`        | Busca una factura por su ID                     | ADMIN, BACK_USER   |
| **GET**   | `/api/v1/factura/entidad/{rut}`            | Busca facturas por RUT de entidad               | ADMIN, BACK_USER   |
| **GET**   | `/api/v1/factura/periodo`                  | Busca facturas por período                      | ADMIN, BACK_USER   |
| **GET**   | `/api/v1/factura/search`                   | Busca facturas por criterios específicos        | ADMIN, BACK_USER   |
| **GET**   | `/api/v1/factura/estadisticas`             | Obtiene estadísticas de facturas                | ADMIN, BACK_USER   |
| **POST**  | `/api/v1/factura/created-factura`          | Crea una nueva factura                          | ADMIN              |
| **PUT**   | `/api/v1/factura/update-factura/{id}`      | Actualiza una factura existente                 | ADMIN              |
| **PATCH** | `/api/v1/factura/updated-patch/{id}/estado`| Actualiza parcialmente el estado de una factura | ADMIN              |
| **DELETE**| `/api/v1/factura/delete-factura/{id}`      | Elimina una factura                             | ADMIN              |

## 🤝 Contribución
1. Fork el proyecto
2. Crea tu rama (`git chekout -b feature/AmazingFeature`)
3. Commit tus cambios (`git commit -m 'Add some AmazingFeature`)
4. Push a la rama (`git push origin feature/AmazingFeature`)
5. Abre un Pull Rquest

# Guías de Estilo
- Sigue las convenciones de Java (Oracle)
- Documenta todas las clases y métodos públicos
- Escribe pruebas unitarias para nueva funcionalidad
- Mantén el código limpio y modular

## 📄 Licencia
Distribuido bajo la licencia MIT. Ver `LICENSE` para más información

## 📞 Soporte
- email: esteban.hernan.lobos@gmail.com 

## 🏆 Agradecimientos
- Apache POI team por la excelente librería
- Supabase por la plataforma increíble
- Comunidad Open Source

#

⭐ Si este proyecto te ha sido útil, no olvides darle una estrella en GitHub!