# LedgerFlow API

**Video:** https://youtu.be/O4N-BQhcbxc

API REST para llevar la contabilidad personal: cuentas, ingresos, egresos, transferencias entre cuentas propias, presupuestos, metas de ahorro y transacciones recurrentes. Cada operación queda registrada en una bitácora.

**La operación principal es ejecutar una transacción recurrente** (`POST /api/ejecuciones`). Es atómica: si algo falla a la mitad se deshace todo, y el intento queda registrado en la bitácora como FALLIDO en una transacción aparte.

**Tecnologías:** Java 21, Spring Boot 4.1, Spring Security con JWT (HS256), Spring Data JPA, PostgreSQL 17, Swagger (springdoc), JUnit y Mockito.

## Cómo levantarlo desde cero

Solo necesita **Docker Desktop**. Los comandos son para PowerShell y se ejecutan dentro de la carpeta `api`.

1. Cree el archivo `.env` y reemplace el valor de `JWT_CLAVE` por una clave de 32 bytes o más. Este archivo no se sube a GitHub.

   ```powershell
   Copy-Item .env.example .env
   ```

2. Levante la API y la base de datos:

   ```powershell
   docker compose up --build
   ```

3. Abra Swagger en <http://localhost:8080/swagger-ui.html>.

4. Inicie sesión con `POST /api/auth/login` usando uno de los [usuarios de prueba](#usuarios-de-prueba), copie el `token`, presione **Authorize** y péguelo.

El puerto 8080 de su máquina debe estar libre. Si está ocupado, cambie `"8080:8080"` por otro puerto en `docker-compose.yml`, por ejemplo `"8081:8080"`.

> **La base se recrea en cada arranque.** Con `spring.jpa.hibernate.ddl-auto=create`, al iniciar la API se borran las tablas y se vuelven a cargar los datos de `import.sql`.

### Ejecutarlo sin contenedor para la API

Requiere JDK 21. La base se levanta con Compose, publicada en el puerto 5435:

```powershell
docker compose up -d db
$env:DB_URL = "jdbc:postgresql://localhost:5435/ledgerflow"
$env:JWT_CLAVE = "una-clave-de-32-bytes-o-mas-para-desarrollo"
.\mvnw.cmd spring-boot:run
```

| Variable | Obligatoria | Valor por defecto |
| :--- | :---: | :--- |
| `JWT_CLAVE` | Sí | No tiene |
| `DB_URL` | No | `jdbc:postgresql://localhost:5432/ledgerflow` |
| `DB_USUARIO` | No | `banco` |
| `DB_CLAVE` | No | `banco123` |

### Probarlo desde la terminal

```powershell
$sesion = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/login `
  -ContentType "application/json" `
  -Body '{"email":"carlos.mendez@example.com","password":"password123"}'

Invoke-RestMethod -Uri http://localhost:8080/api/cuentas `
  -Headers @{ Authorization = "Bearer $($sesion.token)" }
```

## Usuarios de prueba

La contraseña de todos es `password123`. Se cargan desde `src/main/resources/import.sql`.

| Id | Correo | Roles | Cuentas                                                   |
| :---: | :--- | :--- |:----------------------------------------------------------|
| 1 | `diego.garcia@example.com` | ADMINISTRADOR, CLIENTE | `LF-DG-USD-001` (\$1,014.50) y `LF-DG-USD-002` (\$400.00) |
| 2 | `ana.lopez@example.com` | AUDITOR | `LF-AL-EUR-001` (€875.25)                                 |
| 3 | `carlos.mendez@example.com` | CLIENTE | `LF-CM-USD-001` ($250.00)                                 |

Diego y Ana tienen una transacción recurrente lista para ejecutarse el día que se carga la base, porque su próxima ejecución es `CURRENT_DATE`.

## Seguridad

Todas las rutas exigen `Authorization: Bearer <token>`, excepto el login y Swagger. El token dura 1 hora.

| Rol | Qué puede hacer |
| :--- | :--- |
| **ADMINISTRADOR** | Administra catálogos y roles, bloquea y cierra cuentas, y opera sobre los datos de cualquier usuario |
| **CLIENTE** | Gestiona únicamente sus propios datos y registra sus movimientos |
| **AUDITOR** | Solo consulta: la bitácora, sus datos y los de cualquier usuario. No modifica nada |

**El usuario sale del token.** El id se toma del claim `idUsuario` y no se envía en la ruta ni en el JSON. Si un JSON incluye un `usuarioId`, se ignora.

Los endpoints que trabajan con datos de un usuario aceptan un parámetro opcional, `?usuarioId=`:

| Quién lo envía con el id de otro usuario | Consultas (`GET`) | Operaciones (`POST`, `PUT`, `PATCH`, `DELETE`) |
| :--- | :---: | :---: |
| ADMINISTRADOR | Permitido | Permitido |
| AUDITOR | Permitido | 403 |
| CLIENTE | 403 | 403 |

### Respuestas de error

Todos los errores se devuelven como `ProblemDetail` (`application/problem+json`).

| Código | Cuándo ocurre |
| :---: | :--- |
| 400 | Se incumple una regla de negocio, o los datos no son válidos |
| 401 | Falta el token, no es válido o venció, o las credenciales del login son incorrectas |
| 403 | El rol no permite la operación, o se pidieron datos de otro usuario sin permiso |
| 404 | El recurso no existe o no pertenece al usuario |
| 409 | La operación entra en conflicto con datos relacionados que ya existen |

Un recurso ajeno responde igual que uno inexistente, para no revelar qué ids existen.

## Reglas de negocio

| # | Regla | Dónde se cumple | Si no se cumple |
| :---: | :--- | :--- | :--- |
| R1 | Todo monto, límite de presupuesto y objetivo de ahorro es mayor que cero | Validación, servicio y `CHECK` en la tabla | 400 |
| R2 | El saldo nunca es negativo: un egreso o transferencia no puede superar el saldo de origen | Servicio y `CHECK saldo >= 0` | 400 «Saldo insuficiente» |
| R3 | Solo se opera con cuentas ACTIVAS | Servicio | 400 «La cuenta [número] está [estado]» |
| R4 | Un usuario solo opera con sus propios recursos | Servicio | 404 «El recurso no pertenece al usuario» |
| R5 | En una transferencia, origen y destino son cuentas distintas | Servicio | 400 «Las cuentas de origen y destino deben ser distintas» |
| R6 | En una transferencia, ambas cuentas tienen la misma moneda | Servicio | 400 «No se puede transferir entre [moneda] y [moneda]» |
| R7 | La categoría coincide con el tipo de movimiento, y las transferencias no llevan categoría | Servicio | 400 «La categoría no corresponde al tipo de transacción» |
| R8 | El DUI no se repite | `UNIQUE` en la tabla | La base rechaza el registro |
| R9 | No se repiten correo, número de cuenta, referencia de transacción, código de moneda, nombre de tipo de cuenta ni nombre de rol | `UNIQUE` en la tabla | La base rechaza el registro |
| R10 | Una recurrente solo se ejecuta si está ACTIVA y su próxima ejecución es hoy o una fecha anterior | Servicio | 400 «La transacción recurrente está [estado]» o «Aún no le toca ejecutarse» |
| R11 | Al ejecutarse, la próxima ejecución avanza 1 día, 7 días, 1 mes o 1 año según su frecuencia | Servicio | Se deshace todo |
| R12 | Un egreso no puede superar el límite del presupuesto ACTIVO de su categoría en el período | Servicio | 400 «El egreso supero el presupuesto de [categoría]» |
| R13 | Una meta en estado ACTIVO pasa a COMPLETADO cuando el saldo de su cuenta alcanza el objetivo | Servicio, en cada cambio de saldo | Se deshace todo |
| R14 | El saldo de la cuenta cambia solo por créditos y débitos | Entidad `Cuenta` y servicio | Se deshace todo |
| R15 | Cada detalle guarda el saldo de la cuenta justo después de aplicarse | Servicio | Se deshace todo |
| R16 | El monto de la transacción coincide con el de cada uno de sus detalles | Servicio | Se deshace todo |
| R17 | Solo una transacción COMPLETADA puede revertirse, y una sola vez | Servicio | 400 «La transacción ya fue REVERTIDA» |
| R18 | Un movimiento no se modifica ni se borra: un error se corrige con otra transacción | La API no ofrece rutas para modificarlo o borrarlo | No aplica |

El formato del DUI (`00000000-0`) no se valida porque la API no tiene un endpoint para crear usuarios. Los usuarios se cargan desde `import.sql`.

**Otras validaciones:**

- Una cuenta nueva requiere que su tipo y su moneda estén activos, y una cuenta con fondos no se puede cerrar.
- Solo se pueden presupuestar categorías de EGRESO, y la fecha de fin no puede ser anterior a la de inicio.
- La próxima ejecución de una recurrente no puede estar en el pasado, y una recurrente FINALIZADA no se puede modificar.
- El nombre de una categoría no se repite para el mismo usuario, y una categoría con datos relacionados no se puede eliminar (409).

## Endpoints

También están documentados en Swagger, en `/swagger-ui.html`. En la columna **Acceso**:

- **Todos:** AUDITOR, CLIENTE y ADMINISTRADOR.
- **Operación:** CLIENTE y ADMINISTRADOR.
- **Admin:** solo ADMINISTRADOR.

La marca ✓ en `usuarioId` indica que el endpoint acepta ese parámetro opcional.

### Autenticación

| Método | Ruta | Descripción | Acceso |
| :--- | :--- | :--- | :--- |
| POST | `/api/auth/login` | Iniciar sesión y obtener el token | Público |

### Operación principal y movimientos

| Método | Ruta | Descripción | Acceso | `usuarioId` |
| :--- | :--- | :--- | :--- | :---: |
| POST | `/api/ejecuciones` | Ejecutar una transacción recurrente | Operación | ✓ |
| POST | `/api/movimientos/ingresos` | Registrar un ingreso | Operación | ✓ |
| POST | `/api/movimientos/egresos` | Registrar un egreso | Operación | ✓ |
| POST | `/api/movimientos/transferencias` | Transferir entre cuentas propias | Operación | ✓ |
| POST | `/api/movimientos/reversiones` | Revertir una transacción | Operación | ✓ |

Ejemplo de ejecución, que responde 201 con el comprobante:

```json
{ "idTransaccion": 1 }
```

### Cuentas

| Método | Ruta | Descripción | Acceso | `usuarioId` |
| :--- | :--- | :--- | :--- | :---: |
| POST | `/api/cuentas` | Crear una cuenta | Operación | ✓ |
| GET | `/api/cuentas` | Listar las cuentas | Todos | ✓ |
| GET | `/api/cuentas/{cuentaId}` | Obtener una cuenta | Todos | ✓ |
| GET | `/api/cuentas/{cuentaId}/movimientos` | Consultar el historial, paginado | Todos | ✓ |
| PATCH | `/api/cuentas/{cuentaId}/bloqueo` | Bloquear una cuenta | Admin | |
| PATCH | `/api/cuentas/{cuentaId}/desbloqueo` | Desbloquear una cuenta | Admin | |
| PATCH | `/api/cuentas/{cuentaId}/cierre` | Cerrar una cuenta | Admin | |

### Categorías

| Método | Ruta | Descripción | Acceso | `usuarioId` |
| :--- | :--- | :--- | :--- | :---: |
| POST | `/api/categorias` | Crear una categoría | Operación | ✓ |
| GET | `/api/categorias` | Listar; filtro opcional `?tipo=` | Todos | ✓ |
| GET | `/api/categorias/{categoriaId}` | Obtener una categoría | Todos | ✓ |
| PUT | `/api/categorias/{categoriaId}` | Actualizar una categoría | Operación | ✓ |
| DELETE | `/api/categorias/{categoriaId}` | Eliminar una categoría sin dependencias | Operación | ✓ |

### Presupuestos

| Método | Ruta | Descripción | Acceso | `usuarioId` |
| :--- | :--- | :--- | :--- | :---: |
| POST | `/api/presupuestos` | Crear un presupuesto | Operación | ✓ |
| GET | `/api/presupuestos` | Listar; filtro opcional `?estado=` | Todos | ✓ |
| GET | `/api/presupuestos/{presupuestoId}` | Obtener un presupuesto | Todos | ✓ |
| GET | `/api/presupuestos/{presupuestoId}/avance` | Consultar el avance | Todos | ✓ |
| PUT | `/api/presupuestos/{presupuestoId}` | Actualizar un presupuesto | Operación | ✓ |
| PATCH | `/api/presupuestos/{presupuestoId}/activacion` | Activar un presupuesto | Operación | ✓ |
| PATCH | `/api/presupuestos/{presupuestoId}/inactivacion` | Inactivar un presupuesto | Operación | ✓ |

### Metas de ahorro

| Método | Ruta | Descripción | Acceso | `usuarioId` |
| :--- | :--- | :--- | :--- | :---: |
| POST | `/api/metas-ahorro` | Crear una meta | Operación | ✓ |
| GET | `/api/metas-ahorro` | Listar; filtro opcional `?estado=` | Todos | ✓ |
| GET | `/api/metas-ahorro/{metaId}` | Obtener una meta | Todos | ✓ |
| PUT | `/api/metas-ahorro/{metaId}` | Actualizar una meta | Operación | ✓ |
| PATCH | `/api/metas-ahorro/{metaId}/cancelacion` | Cancelar una meta | Operación | ✓ |

### Transacciones recurrentes

| Método | Ruta | Descripción | Acceso | `usuarioId` |
| :--- | :--- | :--- | :--- | :---: |
| POST | `/api/recurrentes` | Crear una recurrente | Operación | ✓ |
| GET | `/api/recurrentes` | Listar; filtro opcional `?estado=` | Todos | ✓ |
| GET | `/api/recurrentes/pendientes` | Listar las pendientes | Todos | ✓ |
| GET | `/api/recurrentes/{recurrenteId}` | Obtener una recurrente | Todos | ✓ |
| PUT | `/api/recurrentes/{recurrenteId}` | Actualizar una recurrente | Operación | ✓ |
| PATCH | `/api/recurrentes/{recurrenteId}/pausa` | Pausar una recurrente | Operación | ✓ |
| PATCH | `/api/recurrentes/{recurrenteId}/reanudar` | Reanudar una recurrente | Operación | ✓ |
| PATCH | `/api/recurrentes/{recurrenteId}/finalizacion` | Finalizar una recurrente | Operación | ✓ |

### Bitácora

| Método | Ruta | Descripción | Acceso |
| :--- | :--- | :--- | :--- |
| GET | `/api/bitacora` | Buscar; filtros opcionales `?usuarioId=`, `?accion=`, `?resultado=`, `?desde=` y `?hasta=` | ADMINISTRADOR y AUDITOR |
| GET | `/api/bitacora/entidad/{entidad}/{entidadId}` | Consultar el historial de una entidad | ADMINISTRADOR y AUDITOR |

### Catálogos

| Método | Ruta | Descripción | Acceso |
| :--- | :--- | :--- | :--- |
| POST | `/api/monedas` | Crear una moneda | Admin |
| GET | `/api/monedas` | Listar; `?activas=false` incluye las inactivas | Todos |
| GET | `/api/monedas/{codigo}` | Obtener una moneda | Todos |
| PUT | `/api/monedas/{codigo}` | Actualizar una moneda | Admin |
| PATCH | `/api/monedas/{codigo}/activacion` | Activar una moneda | Admin |
| PATCH | `/api/monedas/{codigo}/desactivacion` | Desactivar una moneda | Admin |
| POST | `/api/tipos-cuenta` | Crear un tipo de cuenta | Admin |
| GET | `/api/tipos-cuenta` | Listar; `?activas=false` incluye los inactivos | Todos |
| GET | `/api/tipos-cuenta/{nombre}` | Obtener un tipo de cuenta | Todos |
| PUT | `/api/tipos-cuenta/{nombre}` | Actualizar un tipo de cuenta | Admin |
| PATCH | `/api/tipos-cuenta/{nombre}/activacion` | Activar un tipo de cuenta | Admin |
| PATCH | `/api/tipos-cuenta/{nombre}/desactivacion` | Desactivar un tipo de cuenta | Admin |
| POST | `/api/roles` | Crear un rol | Admin |
| GET | `/api/roles` | Listar los roles | Admin |
| PUT | `/api/roles/{nombre}` | Actualizar un rol | Admin |
| PATCH | `/api/roles/{nombre}/activacion` | Activar un rol | Admin |
| PATCH | `/api/roles/{nombre}/desactivacion` | Desactivar un rol | Admin |

## Los logs

La **bitácora** y el **log** son distintos: la bitácora es la auditoría del negocio (tabla en la base, quién hizo qué y si fue EXITOSA o FALLIDA, la consulta el auditor por la API); el log es el diagnóstico técnico (consola o archivo, lo leen soporte y operaciones, y rota).

Se usa SLF4J con Logback, que ya vienen con Spring Boot. Nivel por defecto: INFO.

| Clase | Qué escribe |
| :--- | :--- |
| `OperacionAuditada` | `INFO` cuando una operación (ejecución, ingreso, egreso, transferencia, reversión) se realiza; `WARN` cuando se rechaza, con el motivo; `ERROR` si falla el registro en la bitácora |
| `EjecutorRecurrenteServicio` | `INFO` cuando una recurrente se ejecuta (referencia y próxima ejecución); `DEBUG` al iniciarla |
| `MotorMovimiento` | `INFO` por cada transacción aplicada y por cada meta completada; `DEBUG` con el detalle de la solicitud |
| `EjecutorMovimientoServicio` | `INFO` en reversiones; `WARN` en reversiones y transferencias rechazadas por regla |
| `AutenticacionServicio` | `INFO` en login correcto (id de usuario); `WARN` en login rechazado (correo, nunca la clave) |
| `CuentaServicio` | `INFO` al crear, bloquear, desbloquear y cerrar cuentas; `ERROR` si no se puede generar un número único |
| `BitacoraServicio` | `DEBUG` por cada registro y consulta de bitácora |
| `RespuestasSeguridad` y `UsuarioAutenticado` | `WARN` en cada 401, 403, token sin id e intento de acceder a datos de otro usuario |
| `ManejadorErrores` | `ERROR` con la traza completa para lo inesperado; el cliente solo recibe un 500 genérico |

Nunca se escriben claves, tokens, hashes ni datos personales completos.

Para leerlos: consola de IntelliJ, o `docker compose logs -f api`. El archivo (`logging.file.name`) y el formato JSON (`logging.structured.format.console=ecs`) están comentados en `application.properties`; se activan quitando el `#`.

## Pruebas

Son 40 pruebas. Las que levantan la aplicación necesitan PostgreSQL en `localhost:5432` con una base llamada `ledgerflow-test`, separada de la base de trabajo. No hace falta definir `JWT_CLAVE`, porque las pruebas traen su propia clave.

Si no tiene PostgreSQL en ese puerto, créelo con Docker:

```powershell
docker run -d --name ledgerflow-test-db -e POSTGRES_DB=ledgerflow-test -e POSTGRES_USER=banco -e POSTGRES_PASSWORD=banco123 -p 5432:5432 postgres:17-alpine
```

Si ya tiene un contenedor de PostgreSQL en ese puerto, con el usuario `banco`, basta con crear la base:

```powershell
docker exec <nombre-del-contenedor> createdb -U banco ledgerflow-test
```

Después ejecute:

```powershell
.\mvnw.cmd test
```

| Clase | Pruebas | Necesita base | Qué comprueba |
| :--- | :---: | :---: | :--- |
| `EjecutarRecurrenteReglasTest` | 15 | No | Con Mockito, el flujo de ejecución y las reglas R2, R3, R4, R7, R10, R11, R12 y R13 |
| `UsuarioAutenticadoTest` | 10 | No | Qué id se resuelve desde el token según el rol, y el rechazo de tokens sin `idUsuario` |
| `EjecucionRecurrenteRollbackTest` | 1 | Sí | Si falla el registro en la bitácora, el saldo, la transacción y la próxima ejecución vuelven a su estado anterior |
| `LedgerFlowApplicationTests` | 14 | Sí | Login, 401 sin token o con token inválido, 403 por rol, 403 al pedir otro usuario, JSON con otro usuario, recursos ajenos, y permisos de ADMINISTRADOR y AUDITOR |
