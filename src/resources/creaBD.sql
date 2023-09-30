
--LO TENGO ESCRITO EN ESTE FORMATO DE "COLUMNAS" PARA AYUDARME A VISUALIZARLO, NO ES NECESARIO TANTOS ESPACIOS

CREATE TABLE IF NOT EXISTS COCHE (
    N_BASTIDOR INTEGER     NOT NULL PRIMARY KEY,
    MATRICULA  VARCHAR(7)  NOT NULL            ,
    MARCA      VARCHAR(15) NOT NULL            ,
    MODELO     VARCHAR(15) NOT NULL            ,
    COLOR      VARCHAR(15) NOT NULL)           ;

CREATE TABLE IF NOT EXISTS CONDUCTOR (
    NSS       INTEGER     NOT NULL PRIMARY KEY,
    NOMBRE    VARCHAR(30) NOT NULL            ,
    APELLIDOS VARCHAR(45) NOT NULL)           ;

CREATE TABLE IF NOT EXISTS TRAYECTO (
    NSS            INTEGER NOT NULL                                                                 ,
    N_BASTIDOR     INTEGER NOT NULL                                                                 ,
    KMS            INTEGER NOT NULL                                                                 ,
    FECHA          DATE    NOT NULL PRIMARY KEY                                                     ,    
    GASTOREPOSTAJE DOUBLE  NOT NULL                                                                 ,
    FOREIGN KEY (NSS)        REFERENCES CONDUCTOR (NSS)        ON UPDATE CASCADE ON DELETE CASCADE  ,
    FOREIGN KEY (N_BASTIDOR) REFERENCES COCHE     (N_BASTIDOR) ON UPDATE CASCADE ON DELETE CASCADE) ;



