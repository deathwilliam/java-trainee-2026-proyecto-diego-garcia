# LedgerFlow

## 1. Nombre y de qué se trata
Sistema backend (Spring Boot + PostgreSQL) para el registro de contabilidad personal: cuentas, ingresos, egresos y transferencias entre cuentas, con presupuestos por categoría y metas de ahorro. Está pensado para personas que quieren llevar el control de su dinero. Cada operación queda registrada en una bitácora de auditoría.


## 2. Quién lo usa

| Rol | Quién es | Qué puede hacer |
|-----|----------|-----------------|
| ADMINISTRADOR | Administrador del sistema | configura catálogos (tipos de cuenta y monedas); gestiona usuarios y roles |
| CLIENTE | Persona dueña de sus finanzas | gestiona solo sus cuentas, categorías, presupuestos y metas; registra ingresos, egresos y transferencias |
| AUDITOR | Revisor | Solo consulta la bitácora y las transacciones; no modifica nada |

Cada cuenta y categoría pertenece a un usuario; los presupuestos se vinculan mediante su categoría,
y las metas y recurrentes mediante su cuenta. Un CLIENTE solo accede a sus propios recursos.

## 3. Las entidades

- **Usuario:** Credenciales, DUI único y estado (ACTIVO / BLOQUEADO).
- **Rol / UsuarioRol:** Roles del sistema y su asignación a usuarios.
- **Moneda:** Catálogo de monedas (código, nombre, símbolo).
- **TipoCuenta:** Catálogo de productos (ahorro, corriente, efectivo, tarjeta).
- **Cuenta:** Cuenta de un usuario, con número único, moneda, saldo y estado.
- **Categoria:** Clasificación de movimientos definida por el usuario.
- **Transaccion:** Operación financiera con referencia única, tipo (INGRESO, EGRESO, TRANSFERENCIA), monto y estado.
- **DetalleTransaccion:** Movimiento (DÉBITO o CRÉDITO) que la operación aplica a una cuenta.
- **Presupuesto:** Límite de gasto de un usuario por categoría y período.
- **MetaAhorro:** Objetivo de ahorro ligado a una cuenta; su avance se calcula con el saldo de esa cuenta frente al monto objetivo.
- **TransaccionRecurrente:** Movimiento periódico programado de una cuenta de tipo INGRESO o EGRESO (frecuencia y próxima ejecución).
- **Bitacora:** Registro de qué operación se hizo, quién, cuándo y cómo terminó (EXITOSO / FALLIDO).

## 4. La operación principal: ejecutar una transacción recurrente

**Entrada:** El id de la `transaccionRecurrente` y el usuario autenticado.

**Validaciones previas:** R3, R4, R10 y, si es un egreso, R2 y R12.

**Cambios, en orden (bajo `@Transactional`):**
1. Inserta la fila en `transaccion` (tipo, monto y categoría tomados de la recurrente; estado COMPLETADA) con su `referencia` única (R9).
2. Inserta en `detalle_transaccion` un DEBITO (egreso) o un CREDITO (ingreso) sobre la cuenta, con su `saldo_resultante` (R15) y cuadrando R16.
3. Actualiza el `saldo` de la `cuenta` (R14).
4. Avanza `proxima_ejecucion` en `transaccion_recurrente` según la frecuencia (R11).
5. Si la cuenta tiene una `meta_ahorro` ACTIVO que ya alcanzó su monto objetivo, la pasa a COMPLETADO (R13).
6. Registra la operación en `bitacora` con resultado EXITOSO.

**Si falla a la mitad:** Si fallara, por ejemplo en el paso 4, se deshace todo. No queda la transacción ni el detalle, el saldo vuelve a lo que era y `proxima_ejecucion` no se mueve. El intento se registra en `bitacora` con resultado FALLIDO, en una transacción aparte para que el rollback no se la lleve.

**Salida:** Un comprobante con la referencia de la transacción, el saldo resultante de la cuenta y la nueva `proxima_ejecucion`.

**Operaciones secundarias:** La transferencia entre cuentas propias (un DEBITO en el origen y un CREDITO en el destino), el ingreso y el egreso manual, y la reversión de una transacción pasan por el mismo flujo atómico.

## 5. Reglas de negocio

| # | Regla | Dónde se cumple | Si no se cumple |
|---|-------|-----------------|-----------------|
| R1 | Todo `monto`, `monto_limite` y `monto objetivo` es mayor que cero | tabla (CHECK) | la base rechaza el INSERT |
| R2 | El `saldo` de una cuenta nunca es negativo: un egreso o una transferencia no puede superar el saldo de la cuenta de origen | servicio + tabla (CHECK saldo >= 0) | se rechaza: "Saldo Insuficiente" |
| R3 | Solo se opera con cuentas en estado ACTIVA | servicio | se rechaza: "La cuenta [número] está BLOQUEADA" |
| R4 | Un CLIENTE solo opera con sus propias cuentas, categorías, presupuestos, metas y recurrentes | servicio | se rechaza: "El recurso no pertenece al usuario" |
| R5 | En una transferencia, la cuenta de origen y la de destino no pueden ser la misma | servicio | se rechaza: "Las cuentas de origen y destino deben ser distintas" |
| R6 | En una transferencia, la cuenta de origen y la de destino tienen la misma `moneda` | servicio | se rechaza: "No se puede transferir entre [moneda_1] y [moneda_2]" |
| R7 | El `tipo` de la categoría coincide con el de la `transaccion`: categoría de EGRESO para egresos, de INGRESO para ingresos; las transferencias no llevan categoría | servicio | se rechaza: "La categoría no corresponde al tipo de transacción" |
| R8 | El `dui` tiene el formato `00000000-0` | tabla (UNIQUE) + servicio (formato) | se rechaza: "Ya existe un usuario con ese DUI" |
| R9 | No se repiten `email`, `numero_cuenta`, `transaccion.referencia`, `moneda.codigo`, `tipo_cuenta.nombre` ni `rol.nombre` | tabla (UNIQUE) | la base rechaza el INSERT |
| R10 | Una `transaccion_recurrente` solo se ejecuta si está ACTIVA y su `proxima_ejecucion` es hoy o una fecha anterior | servicio | se rechaza: "La recurrente está PAUSADA" o "Aún no le toca ejecutarse" |
| R11 | Al ejecutarse, `proxima_ejecucion` avanza 1 día, 7 días, 1 mes o 1 año según la frecuencia (DIARIA, SEMANAL, MENSUAL, ANUAL) | servicio, en la operación principal | se deshace todo |
| R12 | Un egreso no puede hacer que la suma de los egresos con estado COMPLETADA de su `categoria` en el período supere el `monto_limite` del `presupuesto` ACTIVO | servicio | se rechaza: "El egreso supera el presupuesto de [categoría]" |
| R13 | Una `meta_ahorro` ACTIVO pasa a COMPLETADO cuando el `saldo` de su `cuenta` llega al `monto_objetivo` | servicio, en toda operación que cambia el saldo | se deshace la operación completa |
| R14 | `cuenta.saldo` = suma de los CREDITO - suma de los DEBITO de sus `detalle_transaccion` | servicio, en la operación principal | se deshace todo |
| R15 | `detalle_transaccion.saldo_resultante` = `saldo` de la cuenta justo después de aplicar ese movimiento | servicio, en la operación principal | se deshace todo |
| R16 | `transaccion.monto` = el monto de cada uno de sus `detalle_transaccion` | servicio, en la operación principal | se deshace todo |
| R17 | Solo una transacción COMPLETADA puede revertirse, y una sola vez | servicio | se rechaza: "La transacción ya fue REVERTIDA" |
| R18 | Un movimiento no se modifica ni se borra: un error se corrige con otra transacción | servicio | se rechaza: "Los movimientos son inmutables" |

Valores de `bitacora.accion`: EJECUTAR_RECURRENTE, TRANSFERIR, REGISTRAR_MOVIMIENTO, y REVERTIR_TRANSACCION. Valores de `bitacora.resultado`: EXITOSO y FALLIDO

## 6. Lo que no hace
- No convierte entre monedas ni maneja tipos de cambio.
- No genera estados de cuenta en PDF.
- No se conecta con bancos reales ni pasarelas de pago.
- No transfiere entre cuentas de usuarios distintos.
- No ejecuta solo los pagos recurrentes: los registra, pero no hay un proceso automático que los dispare.