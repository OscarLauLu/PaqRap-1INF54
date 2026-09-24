# Flujo recomendado de ramas

## Crear una rama de trabajo

Antes de empezar una tarea, actualizar la rama `develop`:

```powershell
git checkout develop
git pull
```

Crear una rama nueva para los cambios:

```powershell
git checkout -b feature/nombre-de-la-tarea
```

Ejemplos de nombres:

```text
feature/catalogo-productos
feature/registro-clientes
fix/login-token
```

## Subir cambios

Cuando termines tus cambios, revisar el estado:

```powershell
git status
```

Agregar los archivos modificados:

```powershell
git add .
```

Crear el commit:

```powershell
git commit -m "descripcion breve del cambio"
```

Subir la rama al repositorio remoto:

```powershell
git push origin feature/nombre-de-la-tarea
```

Luego, en GitHub, crear un Pull Request desde tu rama hacia `develop`.

## Si aparecen conflictos

Si GitHub indica que hay conflictos, primero actualizar tu rama con los ultimos cambios de `develop`:

```powershell
git checkout develop
git pull
git checkout feature/nombre-de-la-tarea
git merge develop
```

Resolver los archivos marcados con conflicto, probar que el proyecto siga funcionando y luego confirmar la resolucion:

```powershell
git add .
git commit -m "resolve conflictos con develop"
git push origin feature/nombre-de-la-tarea
```

Despues de eso, el Pull Request se actualiza automaticamente.

## Archivos de entorno

No subir el archivo `.env`; solo se sube `.env.example`.
