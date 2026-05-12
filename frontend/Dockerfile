# syntax=docker/dockerfile:1.7
FROM node:24-alpine AS build
WORKDIR /workspace
COPY package*.json ./
RUN --mount=type=cache,target=/root/.npm npm ci --prefer-offline --no-audit
COPY . .
RUN npm run build -- --configuration production

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/frontend/browser /usr/share/nginx/html
EXPOSE 80
