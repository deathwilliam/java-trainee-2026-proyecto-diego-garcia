package sv.ledgerflow.api.servicio;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import sv.ledgerflow.api.dtos.respuesta.ComprobanteDto;
import sv.ledgerflow.api.entidad.Categoria;
import sv.ledgerflow.api.entidad.Cuenta;
import sv.ledgerflow.api.entidad.DetalleTransaccion;
import sv.ledgerflow.api.entidad.MetaAhorro;
import sv.ledgerflow.api.entidad.Moneda;
import sv.ledgerflow.api.entidad.Presupuesto;
import sv.ledgerflow.api.entidad.TipoCuenta;
import sv.ledgerflow.api.entidad.Transaccion;
import sv.ledgerflow.api.entidad.TransaccionRecurrente;
import sv.ledgerflow.api.dtos.solicitud.SolicitudMovimiento;
import sv.ledgerflow.api.entidad.enums.AccionBitacora;
import sv.ledgerflow.api.entidad.enums.EstadoCuenta;
import sv.ledgerflow.api.entidad.enums.EstadoMeta;
import sv.ledgerflow.api.entidad.enums.EstadoPresupuesto;
import sv.ledgerflow.api.entidad.enums.EstadoRecurrente;
import sv.ledgerflow.api.entidad.enums.EstadoTransaccion;
import sv.ledgerflow.api.entidad.enums.Frecuencia;
import sv.ledgerflow.api.entidad.enums.TipoCategoria;
import sv.ledgerflow.api.entidad.enums.TipoMovimiento;
import sv.ledgerflow.api.entidad.enums.TipoRecurrente;
import sv.ledgerflow.api.entidad.enums.TipoTransaccion;
import sv.ledgerflow.api.excepciones.RecursoNoEncontradoExcepcion;
import sv.ledgerflow.api.excepciones.ReglaNegocioException;
import sv.ledgerflow.api.repositorio.CuentaRepositorio;
import sv.ledgerflow.api.repositorio.MetaAhorroRepositorio;
import sv.ledgerflow.api.repositorio.PresupuestoRepositorio;
import sv.ledgerflow.api.repositorio.TransaccionRepositorio;
import sv.ledgerflow.api.repositorio.TransaccionRecurrenteRepositorio;
import sv.ledgerflow.api.util.MotorMovimiento;
import sv.ledgerflow.api.util.ReferenciaGenerator;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EjecutarRecurrenteReglasTest {

    private static final Long RECURRENTE_ID = 15L;
    private static final Long CUENTA_ID = 9L;
    private static final Long USUARIO_ID = 7L;
    private static final Long CATEGORIA_ID = 41L;

    @Mock
    private TransaccionRecurrenteRepositorio recurrenteRepositorio;
    @Mock
    private CuentaRepositorio cuentaRepositorio;
    @Mock
    private PresupuestoRepositorio presupuestoRepositorio;
    @Mock
    private TransaccionRepositorio transaccionRepositorio;
    @Mock
    private MetaAhorroRepositorio metaAhorroRepositorio;
    @Mock
    private BitacoraServicio bitacoraServicio;
    @Mock
    private ReferenciaGenerator referenciaGenerator;
    @Mock
    private MotorMovimiento motorMovimiento;
    @InjectMocks
    private EjecutorRecurrenteServicio servicio;
    
    private MotorMovimiento motorReal;
    private Categoria categoria;
    private Cuenta cuenta;
    private TransaccionRecurrente recurrente;

    @BeforeEach
    void prepararDatos() {
        categoria = new Categoria(TipoCategoria.INGRESO, "Salario", null);
        ReflectionTestUtils.setField(categoria, "id", CATEGORIA_ID);

        TipoCuenta tipoCuenta = new TipoCuenta("Ahorro", "Cuenta de prueba", true);
        Moneda moneda = new Moneda("USD", "Dólar", "$", true);
        cuenta = new Cuenta(null, tipoCuenta, moneda, "CTA-TEST", new BigDecimal("100.00"), EstadoCuenta.ACTIVA);
        ReflectionTestUtils.setField(cuenta, "id", CUENTA_ID);

        recurrente = new TransaccionRecurrente(
                cuenta,
                categoria,
                TipoRecurrente.INGRESO,
                new BigDecimal("20.00"),
                "Movimiento recurrente",
                Frecuencia.SEMANAL,
                LocalDate.now(),
                EstadoRecurrente.ACTIVA
        );
        ReflectionTestUtils.setField(recurrente, "id", RECURRENTE_ID);

        lenient().when(recurrenteRepositorio.buscarParaEjecutar(RECURRENTE_ID, USUARIO_ID))
                .thenReturn(Optional.of(recurrente));
        lenient().when(cuentaRepositorio.findByIdForUpdate(CUENTA_ID)).thenReturn(Optional.of(cuenta));
        lenient().when(presupuestoRepositorio.findVigente(anyLong(), any(LocalDate.class))).thenReturn(List.of());
        lenient().when(metaAhorroRepositorio.findByCuentaIdAndEstado(CUENTA_ID, EstadoMeta.ACTIVO))
                .thenReturn(List.of());
        lenient().when(referenciaGenerator.nueva()).thenReturn("REF-TEST-1");

        motorReal = new MotorMovimiento(
                transaccionRepositorio,
                presupuestoRepositorio,
                metaAhorroRepositorio,
                bitacoraServicio,
                referenciaGenerator
        );
        lenient().when(motorMovimiento.aplicar(any(SolicitudMovimiento.class)))
                .thenAnswer(invocacion -> motorReal.aplicar(invocacion.getArgument(0)));
    }

    @Test
    void ejecutaIngresoYCompruebaTransaccionDetalleSaldoMetaYBitacora() {
        MetaAhorro meta = new MetaAhorro(cuenta, "Fondo", new BigDecimal("120.00"), null);
        when(metaAhorroRepositorio.findByCuentaIdAndEstado(CUENTA_ID, EstadoMeta.ACTIVO)).thenReturn(List.of(meta));

        ComprobanteDto comprobante = servicio.ejecutar(RECURRENTE_ID, USUARIO_ID);

        ArgumentCaptor<Transaccion> transaccionCaptor = ArgumentCaptor.forClass(Transaccion.class);
        verify(transaccionRepositorio).save(transaccionCaptor.capture());
        Transaccion transaccion = transaccionCaptor.getValue();
        DetalleTransaccion detalle = transaccion.getDetalles().getFirst();

        assertEquals("REF-TEST-1", comprobante.referencia());
        assertThat(comprobante.saldoResultante()).isEqualByComparingTo("120.00");
        assertEquals(LocalDate.now().plusWeeks(1), comprobante.proximaEjecucion());
        assertEquals(TipoTransaccion.INGRESO, transaccion.getTipo());
        assertEquals(EstadoTransaccion.COMPLETADA, transaccion.getEstado());
        assertThat(transaccion.getMonto()).isEqualByComparingTo("20.00");
        assertEquals(TipoMovimiento.CREDITO, detalle.getTipoMovimiento());
        assertThat(detalle.getSaldoResultante()).isEqualByComparingTo(cuenta.getSaldo());
        assertThat(detalle.getMonto()).isEqualByComparingTo(transaccion.getMonto());
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("120.00");
        assertEquals(EstadoMeta.COMPLETADO, meta.getEstado());
        verify(bitacoraServicio).registrarExitoso(
                eq(USUARIO_ID),
                eq(AccionBitacora.EJECUTAR_RECURRENTE),
                eq("transaccion"),
                isNull(),
                anyString()
        );
    }

    @Test
    void R2_rechazaEgresoQueSuperaElSaldo() {
        prepararEgreso("100.01");

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.ejecutar(RECURRENTE_ID, USUARIO_ID));

        assertEquals("Saldo insuficiente", error.getMessage());
        verify(transaccionRepositorio, never()).save(any(Transaccion.class));
        verifyNoInteractions(metaAhorroRepositorio, bitacoraServicio, referenciaGenerator);
    }

    @Test
    void R2_permiteEgresoQueIgualaElSaldo() {
        prepararEgreso("100.00");

        servicio.ejecutar(RECURRENTE_ID, USUARIO_ID);

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("0.00");
        verify(transaccionRepositorio).save(any(Transaccion.class));
    }

    @Test
    void R3_rechazaUnaCuentaBloqueada() {
        cuenta.setEstado(EstadoCuenta.BLOQUEADA);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.ejecutar(RECURRENTE_ID, USUARIO_ID));

        assertEquals("La cuenta CTA-TEST está BLOQUEADA", error.getMessage());
        verifyNoInteractions(presupuestoRepositorio, transaccionRepositorio, metaAhorroRepositorio,
                bitacoraServicio, referenciaGenerator);
    }

    @Test
    void R4_rechazaUnaRecurrenteQueNoPerteneceAlUsuario() {
        when(recurrenteRepositorio.buscarParaEjecutar(RECURRENTE_ID, USUARIO_ID)).thenReturn(Optional.empty());

        RecursoNoEncontradoExcepcion error = assertThrows(RecursoNoEncontradoExcepcion.class,
                () -> servicio.ejecutar(RECURRENTE_ID, USUARIO_ID));

        assertEquals("El recurso no pertenece al usuario", error.getMessage());
        verifyNoInteractions(cuentaRepositorio, presupuestoRepositorio, transaccionRepositorio,
                metaAhorroRepositorio, bitacoraServicio, referenciaGenerator);
    }

    @Test
    void R7_rechazaUnaCategoriaDeTipoDistintoAlMovimiento() {
        prepararEgreso("10.00");
        categoria.setTipo(TipoCategoria.INGRESO);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.ejecutar(RECURRENTE_ID, USUARIO_ID));

        assertEquals("La categoría no corresponde al tipo de transacción", error.getMessage());
        verify(transaccionRepositorio, never()).save(any(Transaccion.class));
        verifyNoInteractions(metaAhorroRepositorio, bitacoraServicio, referenciaGenerator);
    }

    @Test
    void R10_rechazaUnaRecurrentePausada() {
        recurrente.setEstado(EstadoRecurrente.PAUSADA);

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.ejecutar(RECURRENTE_ID, USUARIO_ID));

        assertEquals("La transacción recurrente está PAUSADA", error.getMessage());
        verifyNoInteractions(cuentaRepositorio, presupuestoRepositorio, transaccionRepositorio,
                metaAhorroRepositorio, bitacoraServicio, referenciaGenerator);
    }

    @Test
    void R10_rechazaUnaRecurrenteCuyaFechaAunNoLlega() {
        recurrente.setProximaEjecucion(LocalDate.now().plusDays(1));

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.ejecutar(RECURRENTE_ID, USUARIO_ID));

        assertEquals("Aún no le toca ejecutarse", error.getMessage());
        verifyNoInteractions(cuentaRepositorio, presupuestoRepositorio, transaccionRepositorio,
                metaAhorroRepositorio, bitacoraServicio, referenciaGenerator);
    }

    @ParameterizedTest
    @EnumSource(Frecuencia.class)
    void R11_avanzaLaProximaEjecucionSegunLaFrecuencia(Frecuencia frecuencia) {
        recurrente.setFrecuencia(frecuencia);
        LocalDate siguiente = frecuencia.siguiente(recurrente.getProximaEjecucion());

        ComprobanteDto comprobante = servicio.ejecutar(RECURRENTE_ID, USUARIO_ID);

        assertEquals(siguiente, comprobante.proximaEjecucion());
        assertEquals(siguiente, recurrente.getProximaEjecucion());
    }

    @Test
    void R12_rechazaUnEgresoQueSuperaElPresupuesto() {
        prepararEgreso("10.01");
        prepararPresupuesto("100.00", "90.00");

        ReglaNegocioException error = assertThrows(ReglaNegocioException.class,
                () -> servicio.ejecutar(RECURRENTE_ID, USUARIO_ID));

        assertEquals("El egreso supero el presupuesto de Salario", error.getMessage());
        verify(transaccionRepositorio, never()).save(any(Transaccion.class));
        verifyNoInteractions(metaAhorroRepositorio, bitacoraServicio, referenciaGenerator);
    }

    @Test
    void R12_permiteUnEgresoQueLlegaExactamenteAlLimiteDelPresupuesto() {
        prepararEgreso("10.00");
        prepararPresupuesto("100.00", "90.00");

        servicio.ejecutar(RECURRENTE_ID, USUARIO_ID);

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("90.00");
        verify(transaccionRepositorio).save(any(Transaccion.class));
    }

    @Test
    void R13_dejaActivaLaMetaMientrasElSaldoNoAlcanzaElObjetivo() {
        MetaAhorro meta = new MetaAhorro(cuenta, "Fondo", new BigDecimal("120.01"), null);
        when(metaAhorroRepositorio.findByCuentaIdAndEstado(CUENTA_ID, EstadoMeta.ACTIVO)).thenReturn(List.of(meta));

        servicio.ejecutar(RECURRENTE_ID, USUARIO_ID);

        assertEquals(EstadoMeta.ACTIVO, meta.getEstado());
    }

    private void prepararEgreso(String monto) {
        categoria.setTipo(TipoCategoria.EGRESO);
        recurrente.setTipo(TipoRecurrente.EGRESO);
        recurrente.setMonto(new BigDecimal(monto));
    }

    private void prepararPresupuesto(String limite, String acumulado) {
        Presupuesto presupuesto = new Presupuesto(
                categoria,
                new BigDecimal(limite),
                LocalDate.now().minusDays(1),
                LocalDate.now().plusDays(1),
                EstadoPresupuesto.ACTIVO
        );
        when(presupuestoRepositorio.findVigente(eq(CATEGORIA_ID), any(LocalDate.class)))
                .thenReturn(List.of(presupuesto));
        when(transaccionRepositorio.sumarEgresosCompletados(
                eq(CATEGORIA_ID), any(), any())).thenReturn(new BigDecimal(acumulado));
    }
}