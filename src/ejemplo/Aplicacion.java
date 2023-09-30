package ejemplo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.PreparedStatement;
import org.h2.tools.Server;
import static java.lang.System.*;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.util.logging.Level;
import java.util.stream.Collectors;
import utilidades.ES;

/**
 * Clase principal de inicio del programa.
 * 
 * @author Jose Cabello
 */
public class Aplicacion {

    /**
     * Nombre del archivo de base de datos local.
     */
    private static final String DB_NOMBRE = "proyectobase.h2db";
    /**
     * URL para la conexión a la base de datos.
     */
    private static final String URL_CONEXION = "jdbc:h2:./" + DB_NOMBRE;
    /**
     * Driver a utilizar para conectarse a la base de datos.
     */
    private static final String DRIVER = "org.h2.Driver";
    /**
     * Opciones de conexión.
     */
    private static final String PARAMS = ";MODE=MySQL;AUTO_RECONNECT=TRUE";

    /**
     * Path al archivo que contiene la estructura de la base de datos.
     */
    public final static String ESTRUCTURA_DB = "/resources/creaBD.sql";

    /**
     * Path al archivo que contiene la estructura de la base de datos.
     */
    public final static String INSERTA_DB = "/resources/cargaBD.sql";

    /**
     * Método principal de la aplicación. En él se realiza la preparación del
     * entorno antes de empezar. A destacar:
     *
     * - Se carga el driver (Class.forName). - Se establece una conexión con la
     * base de datos (DriverManager.getConnection) - Se crean las tablas, si no
     * están creadas, invocando el método createTables. - Se ejecuta una
     * consulta de prueba
     *
     * @param args
     */
    public static void main(String[] args) {
        boolean driverCargado = false;

        //Carga del driver de la base de datos.
        try {
            Class.forName(DRIVER).getDeclaredConstructor().newInstance();
            driverCargado = true;
        }
        catch (ClassNotFoundException e) {
            err.printf("No se encuentra el driver de la base de datos (%s)\n", DRIVER);
        }
        catch (InstantiationException | IllegalAccessException ex) {
            err.printf("No se ha podido iniciar el driver de la base de datos (%s)\n", DRIVER);
        }
        catch (IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException ex) {
            java.util.logging.Logger.getLogger(Aplicacion.class.getName()).log(Level.SEVERE, null, ex);
        }

        //Si el driver está cargado, aseguramos que podremos conectar.
        if (driverCargado) {
            //Conectamos con la base de datos.
            //El try-with-resources asegura que se cerrará la conexión al salir.
            String[] wsArgs = {"-baseDir", System.getProperty("user.dir"), "-browser"};
            try (Connection con = DriverManager.getConnection(URL_CONEXION + PARAMS, "", "")) {

                // Iniciamos el servidor web interno (consola H2 para depuraciones)
                Server sr = Server.createWebServer(wsArgs);
                sr.start();

                // Presentamos información inicial por consola
                out.println("¡¡Atención!!");
                out.println();
                out.println("Mientras tu aplicación se esté ejecutando \n"
                        + "puedes acceder a la consola de la base de datos \n"
                        + "a través del navegador web.");
                out.println();
                out.println("Página local: " + sr.getURL());
                out.println();
                out.println("Datos de acceso");
                out.println("---------------");
                out.println("Controlador: " + DRIVER);
                out.println("URL JDBC: " + URL_CONEXION);
                out.println("Usuario: (no indicar nada)");
                out.println("Password: (no indicar nada)");

                // Creamos las tablas y algunos datos de prueba si no existen y continuamos
                // en el método crearTablas() se encuentra el Statement 
                if (crearTablas(con) ) {
                    
                    //Verificar si no hay datos
                    boolean hayDatos = hayDatosEnTablasAplicacion(con);
                    
                    //Si no hay datos, insertar los datos en las tablas
                    if (hayDatos) {
                        // Insertar los datos en las tablas de la BD
                        insertarDatosTablas(con);
                    }
                    
                    boolean continuar = true;

                    do {
                        System.out.println();
                        System.out.println();
                        System.out.println("----------------  MENÚ DE LA APLICACIÓN ----------------");
                        System.out.println("------------ ---------------------------- --------------");
                        System.out.println("1 - Consultar conductores");
                        System.out.println("2 - Consultar coches");
                        System.out.println("3 - Consultar suma de gasto total de todos los trayectos");
                        System.out.println("4 - Modificar matrícula de coche");
                        System.out.println("5 - Borrar conductor");
                        System.out.println("6 - Nuevo conductor (Implementación adicional mia)");
                        System.out.println("0 - Salir");
                        System.out.println("--------------------------------------------------------");
                        System.out.println("--------------------------------------------------------");
                        System.out.println();
                        System.out.println();

                        // Leer la opción correspondiente a ejecutar.
                        int opcion = ES.leeEntero("Escriba opción: ", 0, 6);
                        switch (opcion) {
                            case 0:
                                continuar = false;
                                break;

                            case 1:
                                consultarConductores(con);
                                break;

                            case 2:
                                consultarCoches(con);
                                break;

                            case 3:
                                consultarSumaGasto(con) ;
                                break;

                            case 4:
                                modificarMatricula(con);
                                break;

                            case 5:
                                borrarConductor(con);
                                break;
                            case 6:
                                nuevoConductor(con);
                                break;
                        }
                    }
                    while (continuar);

                    // Esperar tecla
                    ES.leeCadena("Antes de terminar, puedes acceder a la consola de H2 para ver y modificar la BD. Pulsa cualquier tecla para salir.");                    
                }
                else {
                    System.err.println("Problema creando las tablas.");
                }

                sr.stop();
                sr.shutdown();

            }
            catch (SQLException ex) {
                err.printf("No se pudo conectar a la base de datos (%s)\n", DB_NOMBRE);
            }
        }

    }

    
    /**
     * Dada una conexión válida, lleva a cabo la creación de la estructura de la
     * base de datos, usando como SQL para la creación el contenido en la
     * constante ESTRUCTURA_DB
     *
     * @param con conexión a la base de datos.
     * @see ESTRUCTURA_DB
     * @return true si se creo la estructura y false en caso contrario.
     */
    public static boolean crearTablas(Connection con) {
        boolean todoBien = false;

        try (Statement consulta = con.createStatement() ) {

            String sqlScript = loadResourceAsString(ESTRUCTURA_DB);
            
            if (sqlScript != null) {
                consulta.execute(sqlScript);
                todoBien = true;
            }
            else {
                System.out.printf("Problema cargando el archivo: %s \n", ESTRUCTURA_DB);
                System.out.printf("Para ejecutar este proyecto no puede usarse 'Run File'\n");
            }

        } catch (SQLException ex) {
            System.err.printf("Problema creando la estructura de la base de datos.\n");
            //ex.printStackTrace();
        }
        
        return todoBien;
    }

    
    /**
     * Dada una conexión válida, lleva a cabo la inserción de datos de la base
     * de datos, usando como SQL para la creación el contenido en la constante
     * INSERTA_DB
     *
     * @param con conexión a la base de datos.
     * @see INSERTA_DB
     * @return true si se creo la estructura y false en caso contrario.
     */
    private static boolean insertarDatosTablas(Connection con) {
        
        boolean todoBien = false;

        try (Statement consulta = con.createStatement() ) {

            String sqlScript = loadResourceAsString(INSERTA_DB);
            
            if (sqlScript != null) {
                consulta.execute(sqlScript);
                todoBien = true;
            }
            else {
                System.out.printf("Problema cargando el archivo: %s \n", INSERTA_DB);
                System.out.printf("Para ejecutar este proyecto no puede usarse 'Run File'\n");
            }

        }
        catch (SQLException ex) {
            System.err.printf("Problema insertando datos en la base de datos.\n");
            //ex.printStackTrace();
        }
        
        return todoBien;
    }

    
    /**
     * Carga un recurso que estará dentro del JAR como cadena de texto.
     *
     * @param resourceName Nombre del recurso dentro del JAR.
     * @return Cadena que contiene el contenido del archivo.
     */
    public static String loadResourceAsString(String resourceName) {
        String resource = null;
        InputStream is = Aplicacion.class.getResourceAsStream(resourceName);
        
        if (is != null) {
            try (InputStreamReader isr = new InputStreamReader(is);
                     BufferedReader br = new BufferedReader(isr);
                ) {
                resource = br.lines().collect(Collectors.joining("\n") );
            }
            catch (IOException ex) {
                System.err.printf("Problema leyendo el recurso como cadena: %S\n ", resourceName);
            }
        }
        return resource;
    }

    
    /**
     * Consultar los conductores de la base de datos
     *
     * @param con Conexión a la BD
     */
    private static void consultarConductores(Connection con) {
        if( con != null){
            try (PreparedStatement consulta = con.prepareStatement("SELECT NSS, NOMBRE, APELLIDOS FROM CONDUCTOR") ){
                ResultSet resultados = consulta.executeQuery();
                
                ES.msgln("--------------Listado de conductores--------------");
                ES.msgln("  NSS    Nombre          Apellidos"                );
                ES.msgln("--------------------------------------------------");
                
                //Recorrer el ResultSet
                while (resultados.next() ) {                    
                    int nss          = resultados.getInt   ("NSS");
                    String nombre    = resultados.getString("NOMBRE");
                    String apellidos = resultados.getString("APELLIDOS");
                    
                    System.out.printf("%5d    %-15s %-24s \n", nss, nombre, apellidos);
                }
            }
            catch (SQLException e) {
                System.err.printf("Se ha producido un error al ejecutar la consulta SQL.");
            }
        }        
    }

    
    /**
     * Modificar la matrícula del coche cuyo número de bástidor se introduzca
     * por teclado.
     *
     * @param con Conexión a la BD
     */
    private static void modificarMatricula(Connection con) {

        if (con != null) {
            
            //Se que el enunciado no me pide tanto, pero se me ocurrió una serie de "features" extras que quedarían bien y he decidido implementarlas
            String textoInicial = "Escriba el número de bastidor cuya matrícula se cambiará:";
            int bastidorMin = Aplicacion.getPrimerNBastidor(con);
            int bastidorMax = Aplicacion.getUltimoNBastidor(con);
            
            int numBastidor = ES.leeEntero(textoInicial, bastidorMin, bastidorMax);
            String nuevaMatricula = solicitarMatricula();            

            try (PreparedStatement consulta = con.prepareStatement("UPDATE COCHE SET MATRICULA =? WHERE N_BASTIDOR =?") ){
                consulta.setString(1, nuevaMatricula);
                consulta.setInt(2, numBastidor);

                int registrosAfectados = consulta.executeUpdate();
                if(registrosAfectados > 0) {
                    ES.msgln("La matrícula ha sido modificada correctamente: " + nuevaMatricula);
                }
                else{
                    ES.msgln("No se ha encontrado ningún coche con el número de bastidor especificado.");
                }
            } 
            catch (SQLException e) {
                System.err.printf("Se ha producido un error al ejecutar la consulta SQL.");
            }       
        }
    }

    
    /**
     * Borrar conductor de la BD
     *
     * @param con Conexión a la BD
     */
    private static void borrarConductor(Connection con) {
        
        if( con != null) {
            
            ES.msg("Escriba el NSS del conductor a borrar:");
            int nss = ES.leeEntero();
            
            try (PreparedStatement consultaLeer   = con.prepareStatement("SELECT NOMBRE, APELLIDOS FROM CONDUCTOR WHERE NSS = ?");
                  PreparedStatement consultaBorrar = con.prepareStatement("DELETE FROM CONDUCTOR WHERE NSS = ?") 
                 ){
                
                consultaLeer.setInt(1, nss);
                
                ResultSet resultado = consultaLeer.executeQuery();
                if (resultado.next() ) {
                    String nombre = resultado.getString("NOMBRE");
                    String apellidos = resultado.getString("APELLIDOS");
                    
                    consultaBorrar.setInt(1, nss);
                
                    int registrosAfectados  = consultaBorrar.executeUpdate();
                    if (registrosAfectados > 0) {
                        System.out.printf ("Borrado conductor con NSS: %d, %s %s \n",nss, nombre, apellidos);
                    }
                    else{
                        ES.msg("No se ha realizado nigún cambio en la base de datos.");
                    }
                }
                else{
                    ES.msg("No se ha ningun conductor con NSS especificado.");
                }
            }
            catch (SQLException e) {
                System.err.printf("Se ha producido un error al ejecutar la consulta SQL.");
            }
        }
    }
    
    
    /**
     * Método que añade un nuevo conductor a la tabla de conductores
     * 
     * @param con Conexión a la BD
     */
    public static void nuevoConductor (Connection con) {
        
        if (con != null) {
            
            ES.msg("Introduce el NSS del nuevo conductor: ");
            int nss = ES.leeEntero(1);
            
            ES.msg("Introduce el nombre del nuevo conductor: ");
            String nombre = ES.leeCadena();
            
            ES.msg("Introduce los apellidos del nuevo conductor: ");
            String apellidos = ES.leeCadena();

            try (PreparedStatement consulta = con.prepareStatement("INSERT INTO CONDUCTOR (nss, nombre, apellidos) VALUES (?,?,?)") ){
                consulta.setInt   (1, nss);
                consulta.setString(2, nombre);
                consulta.setString(3, apellidos);
                
                int registrosAfectados = consulta.executeUpdate();
                if (registrosAfectados > 0) {
                    System.out.printf ("\nConductor ingresado correctamente. \nNSS:       %d \nNombre:    %s \nApellidos: %s \n", nss, nombre, apellidos);
                }
                else {
                    ES.msg("El conductor no ha podido ser ingresado.");
                }
            }
            catch (SQLException e){
                System.err.printf("Se ha producido un error al ejecutar la consulta SQL.");
            }
        }
    }

    
    /**
     * Consultar los coches de la base de datos
     *
     * @param con Conexión a la BD
     */
    private static void consultarCoches(Connection con) {
        if (con != null) {
            try (PreparedStatement consulta = con.prepareStatement("SELECT N_BASTIDOR, MATRICULA, MARCA, MODELO, COLOR FROM COCHE") ){
                ResultSet resultados = consulta.executeQuery();
                
                ES.msgln("----------------------------------------------------------------");
                ES.msgln("N_Bastidor   Matrícula        Marca     Modelo          Color"   );
                ES.msgln("----------------------------------------------------------------");
                
                //Recorrer el ResultSet
                while (resultados.next() ){
                    int nBastidor    = resultados.getInt   ("N_BASTIDOR");
                    String matricula = resultados.getString("MATRICULA");
                    String marca     = resultados.getString("MARCA");
                    String modelo    = resultados.getString("MODELO");
                    String color     = resultados.getString("COLOR");
                    
                    System.out.printf("%10d %11s %12s %10s %14s \n", nBastidor, matricula, marca, modelo, color);
                }
            }
            catch (SQLException e){
                System.err.printf("Se ha producido un error en la consulta SQL.");
            }
        }        
    }
        
    
    /**
     * Consultar la suma del gasto
     * @param con 
     */
    private static void consultarSumaGasto(Connection con) {
        
        if (con != null) {
            try (PreparedStatement consulta = con.prepareStatement("SELECT GASTOREPOSTAJE FROM TRAYECTO") ){
                ResultSet resultados = consulta.executeQuery();
                
                double gastoTotal = 0;
                
                //Recorrer el ResultSet
                while (resultados.next() ){
                    double gastoRepostaje = resultados.getDouble("GASTOREPOSTAJE");
                    gastoTotal += gastoRepostaje;
                }
                System.out.println("--------------------Total de gasto en todos los trayectos--------------------");
                System.out.println("El total de euros de los gastos en repostaje de los trayectos es: " + gastoTotal);
            }
            catch (SQLException e) {
                System.err.printf("Se ha producido un error en la consulta SQL.");
            }
        }
    }
    
    //      METODOS EXTRAS QUE HE QUERIDO IMPLEMENTAR 
    
    /**
     * Método que obtiene el número de la primera entrada de bastidor de la tabla COCHE
     * @param con
     * @return El primer bastidor
     */
    private static int getPrimerNBastidor (Connection con) {
        
        int primerBastidor = 0;
        
        if (con != null) {
            
            try (PreparedStatement consulta = con.prepareStatement("SELECT MIN(N_BASTIDOR) FROM COCHE") ){
               ResultSet resultados = consulta.executeQuery();
               if (resultados.next() ){
                   primerBastidor = resultados.getInt(1);
               }
            }
            catch (SQLException e){
                ES.msg("Se ha producido un error al obtener el primer bastidor.");
            }
        }
        return primerBastidor;
    }
    
    
    /**
     * Método que obtiene el número de la última entrada de la tabla COCHE
     * @param con
     * @return El último bastidor
     */
    private static int getUltimoNBastidor (Connection con) {
        int ultimoBastidor = 0;
        if (con != null) {
            try (PreparedStatement consulta = con.prepareStatement("SELECT MAX(N_BASTIDOR) FROM COCHE") ) {
                ResultSet resultado = consulta.executeQuery();
                
                if (resultado.next() ){
                    ultimoBastidor = resultado.getInt(1);
                }
            } 
            catch (SQLException e) {
                ES.msg("Se ha producido un error al obtener el último bastidor.");
            }
        }
        return ultimoBastidor;
    }
    
    
    /**
     * Método que lee la nueva cadena que será la matrícula.
     * @return La nueva matricula
     */
    private static String solicitarMatricula() {
        String nuevaMatricula;
        
        do {
            ES.msgln("Indica la nueva matrícula: (4 cifras y 3 consonantes. No se admitirán vocales)");
            nuevaMatricula = ES.leeCadena().toUpperCase();
        }
        while (!esMatriculaValida(nuevaMatricula) );
        
        return nuevaMatricula;
    }
    
    
    /**
     * Método que comprueba si la matricula encaja dentro de lo que se considera una matrícula de coche válida.
     * @param matricula
     * @return True si la matricula es válida
     */
    private static boolean esMatriculaValida (String matricula) {
        String expresion = "\\d{4}[B-DF-HJ-NP-TV-Zb-df-hj-np-tv-z]{3}";
        boolean esValido = false;
        
        if (matricula.matches(expresion) ){
             esValido = true;
        }
        else{
            ES.msg("La matrícula introducida no cumple el formato de una matrícula válida. Intentelo de nuevo. \n");
        }
        
        return esValido;         
    } 
    
    
    /**
     * Metodo que comprueba si hay datos en la BD
     * @param con
     * @return False si no hay datos
     */    
    private static boolean hayDatosEnTablasAplicacion(Connection con) throws SQLException {
        
        try (Statement consulta = con.createStatement() ){
             ResultSet resultados = consulta.executeQuery("SELECT COUNT(*) FROM CONDUCTOR");

            if (resultados.next() ){
                int contadorRegistros = resultados.getInt(1);
                return contadorRegistros == 0;
            }
        }
        catch (SQLException e) {
            System.err.println("Se ha producido un error al ejecutar la consulta SQL.");
        }

        return false;
    }
}