# ===========================================
# Stage 1: Node Build
# ===========================================
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files and install dependencies
COPY package.json package-lock.json ./
RUN npm ci

# Copy source code and build
COPY . .
RUN npm run build

# ===========================================
# Stage 2: Nginx Serve
# ===========================================
FROM nginx:alpine

LABEL maintainer="AI Agent Platform Team"

# Copy custom nginx config
COPY docker/nginx/nginx.conf /etc/nginx/nginx.conf

# Copy built assets
COPY --from=builder /app/dist /usr/share/nginx/html

RUN addgroup -S nginx && adduser -S nginx -G nginx
USER nginx

EXPOSE 5173

CMD ["nginx", "-g", "daemon off;"]
