# Lab MCP

Aplicación de ejemplo para gestionar pedidos de supermercado mediante
herramientas MCP (Model Context Protocol). Está construida con Spring Boot,
Spring AI, Spring Data JPA y PostgreSQL.

## Funcionalidades

El servidor expone las siguientes herramientas MCP:

| Herramienta | Descripción |
|---|---|
| `buscarProductos` | Busca productos cuyo nombre contiene un fragmento y devuelve precio y stock. |
| `crearPedido` | Crea un pedido para un cliente en estado `PENDIENTE`. |
| `agregarProducto` | Agrega productos a un pedido pendiente, valida el stock y recalcula el total. |
| `consultarPedido` | Consulta el cliente, estado, total y líneas de un pedido. |
| `cancelarPedido` | Cancela un pedido no entregado y devuelve sus cantidades al stock. |
| `actualizarEstadoPedido` | Avanza el pedido de `PENDIENTE` a `EN_CAMINO` y de `EN_CAMINO` a `ENTREGADO`. |

Las herramientas se implementan en
[`PedidoToolsService`](src/main/java/com/example/labmcp/service/PedidoToolsService.java)
y se registran automáticamente mediante `MethodToolCallbackProvider`.

## Tecnologías

- Java 21
- Spring Boot 3.4.7
- Spring AI 1.1.4
- Spring Data JPA
- PostgreSQL
- Maven Wrapper

## Requisitos

- JDK 21 o superior
- PostgreSQL
- Una base de datos llamada `lab3`

## Configuración

La configuración se encuentra en
[`application.properties`](src/main/resources/application.properties).
Por defecto, la aplicación utiliza:

```text
Host: localhost
Puerto de PostgreSQL: 5432
Base de datos: lab3
Usuario: postgres
Puerto de la aplicación: 8088
```

La contraseña se lee de la variable de entorno `PWD_UDEA`. Antes de iniciar
la aplicación, configure dicha variable:

### Windows PowerShell

```powershell
$env:PWD_UDEA = "su contraseña"
```

### Linux/macOS

```bash
export PWD_UDEA="su contraseña"
```

## Base de datos

El archivo [`schema.sql`](src/main/resources/schema.sql) contiene las tablas
`cliente`, `producto`, `pedido` y `detalle_pedido`, además de datos de prueba.
La aplicación tiene configurado `spring.jpa.hibernate.ddl-auto=update`, por
lo que Hibernate puede actualizar las tablas existentes al iniciar.

Antes de ejecutar el proyecto:

1. Cree la base de datos `lab3` en PostgreSQL.
2. Configure `PWD_UDEA`.
3. Ejecute el esquema y los datos de prueba según su instalación de
   PostgreSQL.

## Ejecución

Desde la raíz del proyecto:

### Windows

```powershell
.\mvnw.cmd spring-boot:run
```

### Linux/macOS

```bash
./mvnw spring-boot:run
```

El servidor queda disponible en el puerto `8088`. El protocolo MCP está
configurado como `streamable` y el transporte STDIO está deshabilitado.

## Compilación y pruebas

```powershell
.\mvnw.cmd clean test
```

El proyecto incluye una prueba de carga del contexto Spring en
[`LabmcpApplicationTests`](src/test/java/com/example/labmcp/LabmcpApplicationTests.java).

## Reglas de negocio

- Los pedidos nuevos comienzan en estado `PENDIENTE`.
- Solo se pueden modificar pedidos pendientes.
- La cantidad agregada debe ser mayor que cero.
- No se permite agregar más unidades que el stock disponible.
- Al agregar un producto se descuenta stock inmediatamente.
- Al cancelar un pedido se devuelve el stock.
- Un pedido entregado no se puede cancelar.
- Cancelar un pedido ya cancelado es una operación idempotente.
- Las transiciones permitidas son:

  ```text
  PENDIENTE -> EN_CAMINO -> ENTREGADO
  ```

## Estructura principal

```text
src/
├── main/
│   ├── java/com/example/labmcp/
│   │   ├── LabmcpApplication.java
│   │   ├── model/          Entidades y estados del dominio
│   │   ├── repository/     Repositorios Spring Data JPA
│   │   └── service/        Herramientas MCP y reglas de negocio
│   └── resources/
│       ├── application.properties
│       └── schema.sql
└── test/
    └── java/               Pruebas automatizadas
```

## Logs

Los logs se escriben en `./logs/labmcp.log`. El nivel de Spring AI está
configurado como `DEBUG` para facilitar el diagnóstico durante el desarrollo.
