FROM node:20-alpine

# Instalar pnpm y librerías de compatibilidad glibc para binarios nativos en Alpine
RUN apk add --no-cache libc6-compat && npm install -g pnpm

# Establecer directorio de trabajo
WORKDIR /app

# Copiar archivos de dependencias
COPY package.json pnpm-lock.yaml ./

# Instalar dependencias
RUN pnpm install

# Copiar el resto del código
COPY . .

# Exponer el puerto de Vite
EXPOSE 5173

# Comando para ejecutar el servidor de desarrollo
CMD ["pnpm", "dev", "--host", "0.0.0.0"]
