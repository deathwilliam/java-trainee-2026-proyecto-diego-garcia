package sv.ledgerflow.api.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sv.ledgerflow.api.entidad.Rol;
import sv.ledgerflow.api.dtos.respuesta.RolDto;
import sv.ledgerflow.api.dtos.solicitud.CrearOActualizarRolDto;
import sv.ledgerflow.api.excepciones.RecursoNoEncontradoExcepcion;
import sv.ledgerflow.api.excepciones.ReglaNegocioException;
import sv.ledgerflow.api.repositorio.RolRepositorio;

import java.util.List;

@Service
public class RolServicio {

    private static final Logger log = LoggerFactory.getLogger(RolServicio.class);

    private final RolRepositorio repositorio;

    public RolServicio(RolRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    public RolDto crearRol(CrearOActualizarRolDto rolDto) {
        if(repositorio.existsByNombre(rolDto.nombre())) {
            throw new ReglaNegocioException("Ya existe un rol con el nombre " + rolDto.nombre());
        }

        Rol rol = new Rol(rolDto.nombre(), true);
        repositorio.save(rol);
        log.info("Rol '{}' creado", rol.getNombre());
        return new RolDto(rol.getNombre());
    }

    public RolDto actualizarRol(String nombre, CrearOActualizarRolDto rolDto) {
        Rol rol = obtenerRolPorNombre(nombre);
        if (repositorio.existsByNombre(rolDto.nombre())) {
            throw new ReglaNegocioException("Ya existe un rol con el nombre " + rolDto.nombre());
        }

        rol.setNombre(rolDto.nombre());
        log.info("Rol '{}' renombrado a '{}'", nombre, rolDto.nombre());
        repositorio.save(rol);
        return new RolDto(rol.getNombre());
    }

    public List<RolDto> obtenerRoles() {
        List<Rol> roles = repositorio.findAll();
        return roles.stream().map(r -> new RolDto(r.getNombre())).toList();
    }

    public RolDto activarRol(String nombre) {
        Rol rol = obtenerRolPorNombre(nombre);
        rol.setActivo(true);
        log.info("Rol '{}' activado", nombre);
        repositorio.save(rol);
        return new RolDto(rol.getNombre());
    }

    public RolDto desactivarRol(String nombre) {
        Rol rol = obtenerRolPorNombre(nombre);
        rol.setActivo(false);
        log.info("Rol '{}' desactivado", nombre);
        repositorio.save(rol);
        return new RolDto(rol.getNombre());
    }

    private Rol obtenerRolPorNombre(String nombre) {
        return repositorio.findByNombre(nombre)
                .orElseThrow(() -> new RecursoNoEncontradoExcepcion("Un Rol con el nombre " + nombre + " no existe"));
    }
}
