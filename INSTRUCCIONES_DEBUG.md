# Instrucciones de Depuración - Problema de Usuario No Encontrado

## Problema
Al intentar guardar una habilidad, el backend devuelve "usuario no encontrado" porque el `idUsuario` enviado no existe en la base de datos.

## Cambios Realizados

### 1. MainActivity.java
- **Modificado**: El login ahora consulta la API `/api/usuarios/all` para obtener el ID real del usuario
- **Se busca**: El usuario por email en la lista de usuarios
- **Se guarda**: El `idUsuario` real en `SharedPreferences` con la clave `"userId"`

### 2. RegisterActivity.java
- **Modificado**: Ahora intenta guardar el `userId` en `SharedPreferences` inmediatamente después del registro exitoso
- **Importante**: Esto depende de que el backend devuelva el objeto usuario con su ID en la respuesta

### 3. form.java
- **Agregados**: Logs detallados para depuración
- **TAG**: `"FormActivity"`

## Pasos para Depurar

### 1. Verificar Registro de Usuario
Cuando registres un usuario nuevo:

```
Logcat Filter: RegisterActivity
```

Busca estas líneas:
- ✅ `"Usuario guardado exitosamente. Response: {...}"`
- ✅ `"userId guardado en SharedPreferences: X"`

**Si NO aparece el userId**: El backend no está devolviendo el ID del usuario en la respuesta del endpoint `/api/usuarios/save`

### 2. Verificar Login
Cuando hagas login:

```
Logcat Filter: MainActivity
```

Busca estas líneas:
- ✅ `"Usuario encontrado - ID: X, Email: usuario@email.com"`
- ✅ `"userId guardado en SharedPreferences: X"`

**Si aparece "Usuario no encontrado en la API"**: El usuario no existe en la base de datos o el email no coincide

### 3. Verificar Carga de Form
Cuando abras el formulario de habilidades:

```
Logcat Filter: FormActivity
```

Busca estas líneas:
- ✅ `"Intentando obtener userId de SharedPreferences..."`
- ✅ `"userId obtenido: X"` (debe ser > 0)
- ✅ `"Usuario logeado correctamente con ID: X"`

**Si userId es -1**: El usuario no se guardó correctamente en el login o registro

### 4. Verificar Guardado de Habilidad
Cuando guardes una habilidad:

```
Logcat Filter: FormActivity
```

Busca estas líneas:
- ✅ `"=== Guardando Habilidad ==="`
- ✅ `"idUsuario: X"` (debe ser el ID real del usuario)
- ✅ `"Habilidad guardada exitosamente - Código: 200"`

**Si aparece error**: Verifica el mensaje de error del backend

## Comandos de Logcat

Para ver todos los logs relevantes:
```bash
adb logcat -s MainActivity:D FormActivity:D RegisterActivity:D
```

Para limpiar logs y empezar de nuevo:
```bash
adb logcat -c
```

## Posibles Causas del Problema

### Causa 1: Usuario no se guardó en la base de datos
**Síntoma**: El registro parece exitoso en Firebase pero falla al guardar en la API

**Solución**: 
1. Verifica que el backend esté corriendo en `http://172.193.118.141:8080`
2. Verifica que el endpoint `/api/usuarios/save` funcione correctamente
3. Usa Postman o similar para probar el endpoint manualmente

### Causa 2: Email no coincide
**Síntoma**: El usuario existe pero no se encuentra por email

**Solución**:
1. Verifica que el email guardado en la API sea exactamente igual (mayúsculas/minúsculas)
2. En el log de login, compara el email que buscas vs los emails en la base de datos

### Causa 3: El backend no devuelve el ID en el registro
**Síntoma**: No hay log `"userId guardado en SharedPreferences"` después del registro

**Solución**:
1. Modifica el backend para que el endpoint `/api/usuarios/save` devuelva el usuario completo con su ID
2. O consulta la API después del registro para obtener el ID por email

### Causa 4: Problema con SharedPreferences
**Síntoma**: El userId se guarda pero no se lee correctamente

**Solución**:
1. Verifica que uses el mismo nombre: `getSharedPreferences("UserSession", MODE_PRIVATE)`
2. Verifica que la clave sea exactamente: `"userId"`

## Prueba Manual Recomendada

1. **Desinstala la app completamente** del dispositivo
2. **Vuelve a instalar** la app
3. **Registra un usuario nuevo** y observa los logs
4. **Haz logout** (si hay opción)
5. **Haz login** con el mismo usuario y observa los logs
6. **Abre el formulario** y observa los logs
7. **Guarda una habilidad** y observa los logs

## Verificar Base de Datos

Consulta directamente la base de datos del backend:
- Tabla `usuarios`: Verifica que el usuario esté guardado
- Campo `idUsuario`: Anota el ID
- Campo `email`: Verifica que coincida exactamente con el email de Firebase

## Solución Temporal (Para Pruebas)

Si necesitas probar rápidamente, puedes hardcodear temporalmente un userId que sepas que existe:

```java
// EN form.java - SOLO PARA PRUEBAS
idUsuario = 1; // Usar el ID de un usuario que sepas que existe
Log.d(TAG, "MODO PRUEBA: Usando userId hardcodeado: " + idUsuario);
```

**IMPORTANTE**: Recuerda eliminar esto después de las pruebas.

## Contacto con el Backend

Pregunta al desarrollador del backend:
1. ¿El endpoint `/api/usuarios/save` devuelve el objeto usuario completo con su ID?
2. ¿Qué formato tiene la respuesta?
3. ¿El endpoint `/api/habilidades/save` valida que el usuario exista antes de guardar?
4. ¿Qué mensaje exacto devuelve cuando no encuentra al usuario?

