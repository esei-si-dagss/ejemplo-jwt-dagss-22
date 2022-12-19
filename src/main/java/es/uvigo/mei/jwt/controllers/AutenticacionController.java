package es.uvigo.mei.jwt.controllers;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.uvigo.mei.jwt.daos.UsuarioDAO;
import es.uvigo.mei.jwt.entidades.Rol;
import es.uvigo.mei.jwt.entidades.Usuario;
import es.uvigo.mei.jwt.seguridad.jwt.UtilidadesJWT;
import es.uvigo.mei.jwt.controllers.dtos.DatosLogin;
import es.uvigo.mei.jwt.controllers.dtos.DatosRegistro;
import es.uvigo.mei.jwt.controllers.dtos.MensajeRespuesta;
import es.uvigo.mei.jwt.controllers.dtos.RespuestaJWT;

@RestController
@RequestMapping(path = "api/auth")
@CrossOrigin(origins = "*")
public class AutenticacionController {
	@Autowired
	PasswordEncoder passwordEncoder;

	@Autowired
	UsuarioDAO usuarioDAO;

	@Autowired
	UtilidadesJWT utilidadesJWT;

	@PostMapping(path = "registro")
	public ResponseEntity<?> registrarUsuario(@RequestBody @Valid DatosRegistro datosRegistro) {
		if (usuarioDAO.existsByLogin(datosRegistro.getLogin())) {
			return ResponseEntity.badRequest().body(new MensajeRespuesta("Error: El login ya existe"));
		}
		if (usuarioDAO.existsByEmail(datosRegistro.getEmail())) {
			return ResponseEntity.badRequest().body(new MensajeRespuesta("Error: El email ya existe"));
		}

		// Crear el usuario
		Usuario usuario = new Usuario(datosRegistro.getLogin(), passwordEncoder.encode(datosRegistro.getPassword()),
				datosRegistro.getNombre(), datosRegistro.getEmail());

		Set<String> roles = datosRegistro.getRoles();
		for (String rol : roles) {
			usuario.anadirRol(Rol.valueOf(rol));

		}
		usuarioDAO.save(usuario);
		return ResponseEntity.ok(new MensajeRespuesta("Usuario registrado"));
	}

	// Autenticacion "manual", busca usuario con el login indicado y compara passwords recibido con el password hasheado almacenando
	@PostMapping(path = "login")
	public ResponseEntity<?> login(@RequestBody @Valid DatosLogin datosLogin) {
		String login = datosLogin.getLogin();
		String password = datosLogin.getPassword();
		Optional<Usuario> usuarioOptional = usuarioDAO.findFirstByLogin(login);
		if (usuarioOptional.isPresent()) {
			// usuario existe
			Usuario usuario = usuarioOptional.get();
			if (passwordEncoder.matches(password, usuario.getPassword())) {
				// password correcto -> devuelve token JWT
				String token = utilidadesJWT.crearTokenJWT(login);
				return ResponseEntity.ok(new RespuestaJWT(token, usuario.getId(), usuario.getLogin(), Collections.emptyList()));
			}
		}
		return ResponseEntity.badRequest()
		                     .body(new MensajeRespuesta("Error en autenticacion"));
	}
}
