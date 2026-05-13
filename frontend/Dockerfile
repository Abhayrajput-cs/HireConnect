# syntax=docker/dockerfile:1.7
FROM node:24-alpine AS build
WORKDIR /workspace
COPY package*.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --prefer-offline --no-audit
COPY . .
RUN npm run build -- --configuration production

FROM nginx:1.27-alpine
ENV API_GATEWAY_UPSTREAM=api-gateway-service:8080
ENV AUTH_SERVICE_UPSTREAM=auth-service:8081
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=build /workspace/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
