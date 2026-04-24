# ===========================================
# Stage 1: Node Build
# ===========================================
FROM node:20-alpine AS builder

WORKDIR /app

# Copy package files and install dependencies (layer caching)
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

# Install wget for health check
RUN apk add --no-cache wget

# Copy custom nginx config
COPY docker/nginx/nginx.conf /etc/nginx/nginx.conf

# Copy built assets
COPY --from=builder /app/dist /usr/share/nginx/html

# Create non-root user
RUN addgroup -S nginx && adduser -S nginx -G nginx \
    && chown -R nginx:nginx /usr/share/nginx/html \
    && touch /var/run/nginx.pid \
    && chown -R nginx:nginx /var/cache/nginx /var/run/nginx.pid

USER nginx

EXPOSE 5173

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=10s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:5173/ || exit 1

CMD ["nginx", "-g", "daemon off;"]
