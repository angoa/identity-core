-- =============================================================================
-- V1 — Creacion de la tabla PERSONS
--
-- Almacena el aggregate Person del sistema de identidad digital ATDT.
-- Cada registro representa una persona fisica con CURP valida registrada
-- en el sistema.
--
-- Autor:   Arquitectura ATDT
-- Version: 1.0.0
-- =============================================================================

CREATE TABLE PERSONS (
    -- -------------------------------------------------------------------------
    -- Identificador unico del registro
    -- UUID generado por el dominio, almacenado como cadena de 36 caracteres
    -- -------------------------------------------------------------------------
                         ID               VARCHAR2(36)   NOT NULL,

    -- -------------------------------------------------------------------------
    -- Clave Unica de Registro de Poblacion
    -- Formato RENAPO oficial — 18 caracteres alfanumericos
    -- -------------------------------------------------------------------------
                         CURP             VARCHAR2(18)   NOT NULL,

    -- -------------------------------------------------------------------------
    -- Nombre completo
    -- -------------------------------------------------------------------------
                         GIVEN_NAME       VARCHAR2(80)   NOT NULL,
                         PATERNAL_SURNAME VARCHAR2(80)   NOT NULL,
                         MATERNAL_SURNAME VARCHAR2(80),

    -- -------------------------------------------------------------------------
    -- Fecha de nacimiento
    -- -------------------------------------------------------------------------
                         BIRTH_DATE       DATE           NOT NULL,

    -- -------------------------------------------------------------------------
    -- Estado del ciclo de vida
    -- Valores: ACTIVE, SUSPENDED, REVOKED
    -- -------------------------------------------------------------------------
                         STATUS           VARCHAR2(20)   NOT NULL,

    -- -------------------------------------------------------------------------
    -- Auditoria
    -- -------------------------------------------------------------------------
                         CREATED_AT       TIMESTAMP      NOT NULL,
                         UPDATED_AT       TIMESTAMP      NOT NULL,

    -- -------------------------------------------------------------------------
    -- Restricciones
    -- -------------------------------------------------------------------------
                         CONSTRAINT PK_PERSONS        PRIMARY KEY (ID),
                         CONSTRAINT UQ_PERSONS_CURP   UNIQUE      (CURP),
                         CONSTRAINT CK_PERSONS_STATUS CHECK       (STATUS IN ('ACTIVE', 'SUSPENDED', 'REVOKED'))
);

-- -----------------------------------------------------------------------------
-- Indices
-- -----------------------------------------------------------------------------
CREATE INDEX IX_PERSONS_CURP   ON PERSONS (CURP);
CREATE INDEX IX_PERSONS_STATUS ON PERSONS (STATUS);

-- -----------------------------------------------------------------------------
-- Comentarios de tabla y columnas
-- -----------------------------------------------------------------------------
COMMENT ON TABLE  PERSONS                  IS 'Registro de personas en el sistema de identidad digital ATDT';
COMMENT ON COLUMN PERSONS.ID               IS 'Identificador unico UUID generado por el dominio';
COMMENT ON COLUMN PERSONS.CURP             IS 'Clave Unica de Registro de Poblacion en formato RENAPO';
COMMENT ON COLUMN PERSONS.GIVEN_NAME       IS 'Nombre de pila de la persona';
COMMENT ON COLUMN PERSONS.PATERNAL_SURNAME IS 'Apellido paterno de la persona';
COMMENT ON COLUMN PERSONS.MATERNAL_SURNAME IS 'Apellido materno de la persona, puede ser nulo';
COMMENT ON COLUMN PERSONS.BIRTH_DATE       IS 'Fecha de nacimiento de la persona';
COMMENT ON COLUMN PERSONS.STATUS           IS 'Estado del ciclo de vida: ACTIVE, SUSPENDED, REVOKED';
COMMENT ON COLUMN PERSONS.CREATED_AT       IS 'Fecha y hora de creacion del registro';
COMMENT ON COLUMN PERSONS.UPDATED_AT       IS 'Fecha y hora de la ultima actualizacion del registro';