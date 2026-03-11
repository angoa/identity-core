# ADR-001 — Imagen base de contenedor para identity-core

| Campo       | Valor                              |
|-------------|------------------------------------|
| Estado      | Aceptado                           |
| Fecha       | 2026-03-11                         |
| Autores     | Equipo Arquitectura                |
| Revisores   | —                                  |

---

## Contexto

`identity-core` es un microservicio de identidad digital que debe desplegarse
en dos entornos:

- **OpenShift** (Red Hat) — entorno principal de producción corporativo
- **Kubernetes vainilla** (EKS/GKE/on-prem) — entornos alternativos de clientes

El servicio ofrece dos modos de ejecución:

- **JVM** — para desarrollo, staging y entornos donde el tiempo de compilación
  nativa no es viable
- **Native** (Mandrel/GraalVM) — para producción, donde el arranque sub-segundo
  y el footprint de memoria son requisitos operacionales

La organización prioriza **seguridad certificada y trazabilidad de CVEs** sobre
tamaño mínimo absoluto, dado que el servicio maneja datos de identidad personal
sujetos a LGPDPPSO.

---

## Decisión

### Imagen JVM — `ubi9/openjdk-21-runtime:1.20`

```
registry.access.redhat.com/ubi9/openjdk-21-runtime:1.20
```

### Imagen Native (stage runtime) — `ubi9-micro:9.4`

```
registry.access.redhat.com/ubi9-micro:9.4
```

### Imagen Native (stage builder) — `ubi9-quarkus-mandrel-builder-image:jdk-21`

```
quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-21
```

Imagen oficial de Quarkus con Mandrel preinstalado, alineada con la versión del
framework. Garantiza `native-image` disponible sin configuración adicional y
elimina el drift entre la versión de Mandrel en el host y en el builder.

---

## Alternativas evaluadas

### Alternativa A — Distroless (Google)

`gcr.io/distroless/java21-debian12` para JVM,
`gcr.io/distroless/base-debian12` para Native.

**Ventajas:**
- Imagen más pequeña que UBI (~5MB menos en runtime)
- Sin shell por defecto — superficie de ataque mínima
- Ampliamente adoptada en ecosistema Kubernetes vainilla

**Desventajas:**
- No está certificada para OpenShift ni para RHEL
- No aparece en el catálogo de Red Hat Advanced Cluster Security (ACS/StackRox)
- Los CVEs se reportan en Debian advisory, no en Red Hat Security Advisories (RHSA)
- Las políticas de imagen de OpenShift empresarial suelen requerir imágenes del
  registry de Red Hat o registries corporativos validados
- Sin soporte comercial Red Hat

**Resultado: descartada** por incompatibilidad con el entorno OpenShift corporativo
y ausencia de certificación RHSA.

---

### Alternativa B — Alpine Linux

`eclipse-temurin:21-jre-alpine` para JVM,
`alpine:3.19` para Native.

**Ventajas:**
- Imagen muy pequeña (~5MB base)
- Amplio ecosistema de herramientas disponibles via `apk`
- Popular en entornos Kubernetes vainilla

**Desventajas:**
- Usa `musl libc` en lugar de `glibc` — el binario nativo de Mandrel/GraalVM
  requiere glibc; compilar para musl requiere una toolchain diferente y aumenta
  la complejidad del pipeline
- No certificada para OpenShift
- Historial de CVEs de alta severidad en musl libc (2021, 2022)
- Sin soporte comercial Red Hat
- Comportamiento de DNS diferente en Kubernetes puede causar problemas con
  la configuración de networking de OpenShift SDN

**Resultado: descartada** por incompatibilidad de musl libc con el binario nativo
de Mandrel y por ausencia de certificación OpenShift.

---

### Alternativa C — UBI9-minimal

`registry.access.redhat.com/ubi9-minimal:9.4`

**Ventajas:**
- Certificada Red Hat
- Incluye `microdnf` para instalar paquetes si se necesitan
- Compatible con OpenShift SCC

**Desventajas:**
- ~100MB vs ~8MB de ubi9-micro — significativamente más grande para el runtime
  nativo donde no se necesitan herramientas de gestión de paquetes
- La presencia de `microdnf` es superficie de ataque innecesaria en producción

**Resultado: descartada para runtime nativo** — se mantiene como opción válida
para el runtime JVM si se necesitan paquetes adicionales en el futuro.

---

### Alternativa D — UBI9 completo

`registry.access.redhat.com/ubi9:9.4`

**Resultado: descartada** — imagen de ~230MB con `dnf`, herramientas de sistema
y compiladores. Apropiada solo para stages de build, no para runtime.

---

## Justificación de la decisión

### UBI9 (Universal Base Image) como estándar

UBI es la base oficial de Red Hat para contenedores que se distribuyen y ejecutan
en entornos empresariales. Todas las imágenes UBI están:

1. **Certificadas para OpenShift** — pasan las validaciones de Red Hat Container
   Certification y aparecen en el catálogo de OperatorHub/Red Hat Ecosystem
2. **Compatibles con SCC restricted** — el UID del proceso es configurable para
   cumplir con la política de OpenShift que prohíbe UID 0
3. **Sujetas a RHSA** — los CVEs se publican en Red Hat Security Advisories con
   NVD score, lo que facilita el compliance con políticas de seguridad corporativas
4. **Redistribuibles sin licencia** — a diferencia de RHEL completo, UBI puede
   usarse y distribuirse libremente en cualquier entorno

### Diferenciación JVM vs Native en imagen base

| | JVM | Native |
|---|---|---|
| Base | `ubi9/openjdk-21-runtime` | `ubi9-micro` |
| Tamaño base | ~220MB | ~8MB |
| Imagen final aprox. | ~280MB | ~65MB |
| Shell | No (runtime-only) | No |
| JVM en runtime | Sí | No |
| Tiempo arranque | ~3-5s | <100ms |
| Memoria RSS reposo | ~150MB | ~50MB |

Para el runtime nativo se usa `ubi9-micro` (equivalente Red Hat de Distroless)
porque el binario nativo no necesita JVM ni herramientas de sistema — solo glibc
y las librerías de C++ que Mandrel enlaza estáticamente en la mayoría de los casos.

### Mandrel sobre GraalVM CE

Mandrel es la distribución de GraalVM mantenida por Red Hat, optimizada
específicamente para Quarkus. Se elige sobre GraalVM CE porque:

- Certificado por Red Hat para uso en OpenShift
- Actualizaciones de seguridad coordinadas con el ciclo de RHEL
- Soporte comercial Red Hat disponible
- Pipeline de Quarkus nativo en OpenShift usa Mandrel por defecto

---

## Consecuencias

**Positivas:**
- Pipeline de seguridad unificado — todos los CVEs se gestionan via RHSA
- Compatible con Red Hat Advanced Cluster Security sin configuración adicional
- Funciona en OpenShift SCC restricted sin modificar el SecurityContext del pod
- Imagen final de producción (~65MB) competitiva con Distroless (~60MB)

**Negativas:**
- Dependencia del registry `registry.access.redhat.com` — requiere acceso a
  internet o mirror interno en entornos air-gapped
- Las actualizaciones de imagen base deben coordinarse con el ciclo de parches
  de Red Hat (aproximadamente mensual)
- En Kubernetes vainilla sin Red Hat ACS, se pierde parte del valor del
  escaneo de CVEs integrado (se puede compensar con Trivy o Grype)

---

## Referencias

- [Red Hat UBI overview](https://www.redhat.com/en/blog/introducing-red-hat-universal-base-image)
- [Quarkus container images guide](https://quarkus.io/guides/container-image)
- [Mandrel — GraalVM for Quarkus](https://github.com/graalvm/mandrel)
- [OpenShift SCC restricted policy](https://docs.openshift.com/container-platform/4.14/authentication/managing-security-context-constraints.html)
- [ubi9-micro documentation](https://catalog.redhat.com/software/containers/ubi9-micro/61832888c0f9b7a4f8c04d9b)