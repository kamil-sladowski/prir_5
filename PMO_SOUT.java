
public class PMO_SOUT {
	private final static String iniS = "PMO: ";
	private static boolean newLine = true;

	private static void testNewLine() {
		if ( newLine ) {
			System.out.print( iniS );
			newLine = false;
		}		
	}

	synchronized public static void println( String p ) {
		testNewLine();
		System.out.println( p );
		newLine = true;
	}
	
	synchronized public static void print( String p ) {
		testNewLine();
		System.out.print( p );
	}
}
