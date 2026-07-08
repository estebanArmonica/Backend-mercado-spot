## Sistema de Migración ETL Mercado Spot Backend

Este es un backend realizado con spring boot java en version 17 el cual tiene el fin de crear un sistema ETL de migración de datos en excel a bases de datos relacionales, utilizando sistema de principios SOLID y lenguaje POO Avanzado.

Además se incluyo autenticaciones por token para seguiridad privada de usuarios de la empresa, como también protección sobre ataques ciberneticos y SQL Injection.

# Herramientas utilizadas para el proyecto:

- Java 17
- Spring Boot 4.0.6
- Visual Studio Code
- PostgreSQL
- Apache POI 5.2.5
- Autenticacion por JWT
- JSON Assert
- Mockito


# Primeros pasos

Para realizar el arranque de este proyecto necesitamos de algunos requisitos, uno de ellos es tener instalado `Maven` para realizar limpieza y depuración de proyecto

> [!NOTE]
> Si no tienes instalado Maven, te recomiendo instalarlo ya que el proyecto se levanta unicamente con comandos de Maven.

En caso de que tengas instalado maven, clone el repositorio del proyecto
```bash
    git clone <repositorio>
    cd <nombre-repo>
```

una vez que esté realizado la clonacion del proyecto, donde de la raiz del proyecto se debe crear un archivo `.env` ya que el proyecto sin las variables de entorno no funcionara correctamente

```bash
    SUPERBASE_URL=database
    SUPERBASE_USER=user
    SUPERBASE_PASS=password

    # Variables del JWT
    JWT_FIRMA=token_max_64 # el token debe tener un largo de 64 como minimo
    JWT_EXPIRATION=000000000000 # cuanto expira el token

```

al agregar estas variables por medio de `cmd` o `powershell` ingresar los siguientes comandos

```bash
    # utilizamos este comando para limpiar el target si esque existe el target
    mvn clean

    # creamos el target para la compilacion de archivo .war
    mvn compile

    # realizamos los test para verificar que cada endpoint del controller funcione correctamente
    mvn test

    # o de forma especifica
    mvn test -Dtest=NombreClaseTest

    # si todo funciona correctamente corremos el proyecto
    # levantamos el proyecto de spring boot
    mvn spring-boot:run 

```

tambien existe un `script.sh` donde se puede levantar el proyecto de spring boot por `git Bash`
