# Prueba Rápida - Verificar Usuario

## Paso 1: Limpiar Logcat
Antes de comenzar, limpia los logs para ver solo los nuevos:
```bash
adb logcat -c
```

## Paso 2: Iniciar Filtro de Logcat
En Android Studio, configura el filtro de Logcat:
- **Package**: `sv.edu.itca.proyecto_dam`
- **Log Level**: Debug
- O usa el filtro: `MainActivity|FormActivity|RegisterActivity`

## Paso 3: Hacer Login
1. Abre la app
2. Ingresa tus credenciales
3. Haz clic en "Login"

## Paso 4: Revisar Logs de Login

Busca estas líneas en Logcat:

```
MainActivity: === RESPUESTA DE /api/usuarios/all ===
MainActivity: Total de usuarios encontrados: X
MainActivity: Usuario #0 - ID: 1, Email: usuario1@example.com
MainActivity: Usuario #1 - ID: 2, Email: usuario2@example.com
MainActivity: ✅ Usuario encontrado - ID: X, Email: tu_email@example.com
MainActivity: ✅ userId guardado en SharedPreferences: X
```

### ❌ Si ves esto:
```
MainActivity: ❌ Usuario NO encontrado con email: tu_email@example.com
```

**Significa**: El usuario NO está en la base de datos del backend.

**Solución**: Necesitas registrar al usuario primero.

## Paso 5: Abrir Formulario de Habilidades
1. Navega al formulario
2. Observa los logs

Busca:
```
FormActivity: Intentando obtener userId de SharedPreferences...
FormActivity: userId obtenido: X
FormActivity: Usuario logeado correctamente con ID: X
```

### ❌ Si ves esto:
```
FormActivity: ERROR: No se encontró userId en SharedPreferences
FormActivity: userId obtenido: -1
```

**Significa**: El userId no se guardó en el paso anterior.

## Paso 6: Guardar Habilidad
1. Llena el formulario
2. Haz clic en "Guardar"
3. Observa los logs

Busca:
```
FormActivity: === Guardando Habilidad ===
FormActivity: idUsuario: X
FormActivity: idCategoria: Y
FormActivity: idNivel: Z
FormActivity: nomHabilidad: Nombre de la habilidad
FormActivity: descripcion: Descripción
FormActivity: Habilidad guardada exitosamente - Código: 200
```

### ❌ Si ves error:
```
FormActivity: Error al guardar habilidad - Código: 400, Body: Usuario no encontrado
```

**Posibles causas**:
1. El usuario no existe en la base de datos (verificar con el backend)
2. El idUsuario es incorrecto
3. El backend tiene un problema

## Verificación Manual del Backend

### Opción 1: Usar un navegador
Abre en tu navegador (o Postman):
```
http://172.193.118.141:8080/api/usuarios/all
```

Deberías ver un JSON con todos los usuarios. Verifica:
- ¿Aparece tu email?
- ¿Qué ID tiene tu usuario?

### Opción 2: Probar el endpoint de habilidades
En Postman, haz un POST a:
```
http://172.193.118.141:8080/api/habilidades/save
```

Con estos parámetros (form-data):
```
idUsuario: [EL ID QUE VISTE EN /api/usuarios/all]
idCategoriaHabilidad: 1
idNivel: 1
nomHabilidad: Prueba
descripcionBreve: Descripción de prueba
```

Si funciona manualmente pero no desde la app, el problema está en cómo la app está obteniendo/enviando el ID.

## Solución Temporal de Emergencia

Si necesitas que funcione YA y el problema es que el usuario no está en la base de datos:

### 1. Registrar el usuario manualmente en el backend
Usa Postman para hacer un POST a:
```
http://172.193.118.141:8080/api/usuarios/save
```

Con estos datos (form-data):
```
nombre: Tu Nombre
apellido: Tu Apellido
email: [EL MISMO EMAIL QUE USAS EN FIREBASE]
password: tu_password
biografia: Mi biografía
```

### 2. O modificar temporalmente form.java

En `form.java`, línea ~50, TEMPORALMENTE cambia:

```java
// TEMPORAL - BORRAR DESPUÉS
idUsuario = 1; // O el ID que veas en /api/usuarios/all
Log.d(TAG, "⚠️ USANDO ID HARDCODEADO: " + idUsuario);
```

**IMPORTANTE**: Esto es SOLO para probar. Debes quitar esto después.

## Siguiente Paso

Una vez que identifiques dónde falla exactamente, avísame y podré ayudarte a resolverlo.

