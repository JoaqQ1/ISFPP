Sistema de Consulta de Colectivos
=================================

Este proyecto es una aplicación de escritorio desarrollada en Java que permite a los usuarios consultar horarios y recorridos de líneas de colectivos urbanos. La aplicación calcula la ruta óptima entre dos paradas, visualiza el recorrido en un mapa y ofrece una interfaz amigable para el usuario.

El sistema está diseñado para ser flexible, permitiendo obtener datos tanto desde archivos de texto locales como desde una base de datos PostgreSQL.

## Captura de Pantalla
![Interfaz de la aplicación de colectivos](screenshots/app_screenshot.png)

Características Principales
---------------------------

*   **Interfaz Gráfica (JavaFX):** Interfaz de usuario intuitiva para seleccionar paradas de origen/destino, día de la semana y hora.
    
*   **Visualización en Mapa:** Muestra el recorrido calculado sobre un mapa interactivo.
    
*   **Cálculo de Recorridos:** Implementa la lógica de negocio para determinar la mejor ruta.
    
*   **Múltiples Fuentes de Datos:** Soporta la carga de datos desde:
    
    *   Archivos de texto (secuencial).
        
    *   Base de datos PostgreSQL.
        
*   **Internacionalización (i18n):** La interfaz está disponible en Español e Inglés.
    
*   **Logging:** Registra el historial de consultas utilizando **Log4j**.
    
*   **Tareas Asíncronas:** Las consultas se ejecutan en segundo plano (usando javafx.concurrent.Task, similar a SwingWorker) para mantener la interfaz fluida y receptiva.
    

Requisitos Previos
------------------

Para compilar y ejecutar este proyecto, necesitarás:

*   **Java JDK 21** (o superior).
    
*   **Apache Maven** (versión 3.6 o superior).
    
*   **PostgreSQL** (Opcional, solo si deseas utilizar la fuente de datos de base de datos).
    
*   Un IDE compatible con Java/Maven, como IntelliJ IDEA, Eclipse o VS Code con el "Extension Pack for Java".
    

Instalación y Ejecución
-----------------------

Sigue estos pasos para poner en marcha la aplicación:

### 1. Clonar el Repositorio

`   git clone https://github.com/JoaqQ1/ISFPP.git  cd colectivo-base   `

### 2. Configuración de la Aplicación (config.properties)

La configuración principal de la aplicación se maneja en el archivo:

` src/main/resources/config.properties `

Aquí puedes definir dos ajustes clave:
##### a)  Fuente de Datos  
Define de dónde leerá los datos la aplicación (archivos de texto o base de datos) usando la clave `persistencia.tipo`.
```cleaned-db.properties
# Para usar los archivos de texto (lectura secuencial), descomenta esta línea:
persistencia.tipo=ARCHIVO

# Para usar la base de datos, descomenta esta línea:
# persistencia.tipo=BD
```
##### b) Ciudad Activa y Mapa

Esta configuración le indica a la aplicación qué conjunto de datos de ciudad cargar (ej. `linea.CO`, `parada.CO` en `secuencial.properties`) y dónde centrar el mapa al iniciar.

**¡Importante!** Para cambiar de ciudad, debes editar el bloque completo. Al seleccionar una `ciudad.actual`, debes asegurarte de que las coordenadas (`origen.latitud`, `origen.longitud`, `zoom.inicial`) también correspondan a esa ciudad.
* **Nota**: Actualmente, el modo `persistencia.tipo=ARCHIVO` solo incluye los datos para `CO` (Caleta Olivia) y `PM` (Puerto Madryn).

**Ejemplo de configuración (Caleta Olivia activa):**
```cleaned-db.properties
# --- Caleta Olivia (CO) ---
ciudad.actual=CO
origen.latitud=-46.4676
origen.longitud=-67.5215
zoom.inicial=4

# --- Puerto Madryn (PM) ---
# ciudad.actual=PM
# origen.latitud=-42.7692
# origen.longitud=-65.0385
# zoom.inicial=4
```
(Para cambiar a Puerto Madryn, solo debes comentar el bloque de CO y descomentar el bloque de PM).

### 3\. Configuración de la Base de Datos (Opcional)

Si seleccionaste `persistencia.tipo=BD` en `config.properties`, debes configurar tu conexión a la base de datos PostgreSQL editando este archivo:
` src/main/resources/db.properties `
##### a) Configurar Credenciales
Asegúrate de que los parámetros de conexión, URL, usuario y contraseña sean correctos.
```cleaned-db.properties
# ===============================================
# CONFIGURACIÓN DE LA BASE DE DATOS (PostgreSQL)
# ===============================================

# -----------------------------------------------
# Parámetros del Servidor y Conexión
# -----------------------------------------------
db.url=jdbc:postgresql://132.255.7.202:30000/bd1
db.driver=org.postgresql.Driver
db.timezone=America/Argentina/Buenos_Aires

# -----------------------------------------------
# Autenticación
# -----------------------------------------------
db.usuario=estudiante
db.contrasena=estudiante
```
##### b) Seleccionar el Schema (Ciudad)
La base de datos utiliza schemas separados para almacenar los datos de cada ciudad. Descomenta la línea del `db.schema` que corresponda a la ciudad que deseas consultar.
```cleaned-db.properties
# -----------------------------------------------
# Selección de Schema (Ciudad)
# -----------------------------------------------
# Define qué schema (conjunto de datos de ciudad) se usará.
# Descomenta el schema de la ciudad que quieras usar.

# --- Caleta Olivia (CO) ---
db.schema="colectivo_CO"

# --- Puerto Madryn (PM) ---
# db.schema="colectivo_PM"
```
##### c) Preparar la Base de Datos
Si estás configurando la base de datos desde cero:

1. Crear la Base de Datos: Asegúrate de que la base de datos (ej. `bd1` en la URL) exista en tu servidor PostgreSQL.

2. Ejecutar Scripts: Importa la estructura de tablas y los datos para cada schema (ej. `colectivo_CO`, `colectivo_PM`) usando los scripts SQL proporcionados en el proyecto.
### 4\. Compilar y Ejecutar con Maven

Una vez configurado, puedes compilar y ejecutar la aplicación usando Maven.
```bash
# 1. Compila el proyecto y descarga las dependencias  
mvn clean install  
# 2. Ejecuta la aplicación (requiere el plugin de JavaFX para Maven)  
mvn exec:java -Dexec.mainClass="colectivo.app.AplicacionConsultas"   
```


Arquitectura y Patrones de Diseño
---------------------------------

El proyecto está estructurado siguiendo patrones de diseño clave para mantener un código limpio, modular y mantenible.

### Modelo-Vista-Controlador (MVC)

*   **Modelo:** Contiene las entidades de negocio (ej. `Linea`, `Parada`, `Recorrido`). Se encuentra en `colectivo.modelo`.
    
*   **Vista:** Compuesta por los archivos FXML (`.fxml`) y las clases de la interfaz gráfica (`colectivo.ui.impl.javafx`).
    
*   **Controlador:** Actúa como coordinador (`CoordinadorApp`) y gestiona la lógica de la interfaz (controladores JavaFX), conectando la Vista con los servicios.
    

### Data Access Object (DAO)

Se utiliza el patrón DAO para abstraer la lógica de persistencia de datos.

*   **Interfaces:** Se definen interfaces como `LineaDAO`, `ParadaDAO`, etc., en `colectivo.persistencia.dao`.
    
*   **Implementaciones:** Existen dos implementaciones concretas:
    
    1.  `colectivo.persistencia.dao.secuencial`: Lee los datos desde archivos de texto.
        
    2.  `colectivo.persistencia.dao.bd`: Lee los datos desde la base de datos PostgreSQL.
        

### Factory Method

Se utiliza una clase "Factory" (ubicada en `colectivo.configuracion`) para instanciar la implementación correcta de los DAO (Secuencial o BD) basándose en lo definido en el archivo `factory.properties`. Esto permite cambiar la fuente de datos de toda la aplicación sin modificar el código fuente.


