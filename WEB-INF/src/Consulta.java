import java.io.*;
import java.util.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import org.w3c.dom.*;
import org.xml.sax.*;


public class Consulta extends HttpServlet{
    public PrintWriter out;
	int count=0;
	Document doc;  //resultado del parse
	String consulta,paso,variable,auxiliar;
	
	public Vector<Document> arboles;
	public Vector<String> xml_leidos;  
	public Vector<String> xml_faltan;
	public Vector<String> peliculas;
	public Vector<String> paises_leidos;
	public Vector<String> num_oscars;
	public Vector<String> actores_directores_leidos;
	public Vector<String> peliculas_leidas;
	public Vector<String> idiomas_leidos;
	public Vector<String> problema;
	
	
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		
		arboles = new Vector<Document>();
		xml_leidos = new Vector<String>();
		xml_faltan = new Vector<String>();
		problema = new Vector<String>();
		
		xml_faltan.addElement("http://clave.det.uvigo.es:8080/~lroprof/11-12/p2/cml10.xml");

		parsear_xml();
	}
	
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws ServletException, IOException{
		
		//consultas: (Value)
		//1.info_pelicula (de un determinado año o toda la historia,primero debe elegir)
		//2.clasificacion_oscars (de un año o toda la historia)
		//3.peliculas_pais_anio (info de todas las peliculas de un pais en un año agrupadas por el idioma original)
		//4.resumen_actor_director (resumen de la carrera de un director)
		
		response.setContentType("text/html");
		out = response.getWriter();
		consulta = request.getParameter("consulta");
		paso = request.getParameter("paso");
		variable = request.getParameter("variable");
		auxiliar = request.getParameter("auxiliar");
		
		
		switch (Integer.parseInt(consulta)) {
			case 1:
				info_pelicula();
				break;
			case 2:
				clasificacion_oscars();
				break;
			case 3:
				pelicula_pais_anio();
				break;
			case 4:
				resumen_actor_director();
				break;
			default:
				break;
		}	
		
	}	
	
	
	/*****************************************************************************************/
	/************************************* CONSULTA 1*****************************************/
	/*****************************************************************************************/
	
	
	public void info_pelicula(){ // La información de una pelicula de un anio concreto
		switch (Integer.parseInt(paso)) {
			case 1:
				anios_disponibles(0);
				break;
			case 2:
				peliculas_del_anio_indicado();
				break;
			case 3:
				buscar_info_pelicula();
				break;
			default:
				break;
		}
	}	
	
	public void anios_disponibles(int tipo){  
		// si TIPO = 0 muestras todos los anios
		// si TIPO = 1 muestra todos los anios y da la opcion de toda la historia
		// en arboles tengo todos los doc para buscar
		
		try{
			enviar_cabecera_html();
			enviar_cuerpo_html();
			
			for (int i=0; i<arboles.size(); i++) {
				
				NodeList nodos_anio = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("/Cine/Anio/text()",arboles.elementAt(i),XPathConstants.NODESET));
				
				for (int j=0; j < nodos_anio.getLength(); j++) { //el for es un poco estupido porque solo puede haber un año
					enviar_boton(nodos_anio.item(j).getNodeValue(),nodos_anio.item(j).getNodeValue());
				}
				
				
			}
			
			if (tipo==1) { enviar_boton("9999","Toda la historia");}
			enviar_boton_submit();
			if (tipo==0) enviar_final_html("1","2","0"); //en la consulta 1, el siguiente paso es enviar las peliculas del anio que llegara (2) 
			if (tipo==1) enviar_final_html("2","2","0"); //en la consulta 2, el siguiente paso es motrar la clasificacion
			//Aqui da igual el auxiliar que se le ponga
		}catch (XPathExpressionException xee) {
			System.err.println(xee);
			System.exit(1);
		}
	}
	
	public void peliculas_del_anio_indicado(){
		
		peliculas = new Vector<String>();
		
		try{
			Document doc = buscar_arbol_por_anio(variable);
			
			NodeList nodos_peliculas = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula/Titulo/text()",doc,XPathConstants.NODESET));
			
			enviar_cabecera_html();
			enviar_cuerpo_html();
			
			for (int i = 0; i < nodos_peliculas.getLength(); i++) {
				peliculas.addElement(nodos_peliculas.item(i).getNodeValue());
				enviar_boton(Integer.toString(peliculas.indexOf(nodos_peliculas.item(i).getNodeValue())),nodos_peliculas.item(i).getNodeValue());
			}
			
			enviar_boton_submit();
			enviar_final_html("1","3",variable); //en la consulta 1, el siguiente paso es enviar la informacion de la pelicula que llegara (3)
		}catch (XPathExpressionException xee) {
			System.err.println(xee);
			System.exit(1);
		}
	}
	
	public Document buscar_arbol_por_anio(String anio){ // Se pasa el string del anio que se quiera buscar
		
		try{
			for (int i=0; i<arboles.size(); i++) {
				
				for (int k=0; k<arboles.size(); k++) {
					NodeList nodos_anio = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("/Cine/Anio/text()",arboles.elementAt(k),XPathConstants.NODESET));
					
					//En este momento la variable tiene que tener el numero de un año
					for (int j=0; j < nodos_anio.getLength(); j++) {
						if (anio.equals(nodos_anio.item(j).getNodeValue())){
							return (arboles.elementAt(k));
						}
					}
				}
				
			}
		}catch (XPathExpressionException xee) {
			System.err.println(xee);
			System.exit(1);
		}
		// NUNCA DEBERIA LLEGAR HASTA AQUI!!!!!!!!!!
		return(arboles.elementAt(-1)); //ERROR
	}
	
	public void buscar_info_pelicula(){ //En este momento la variable tiene que tener el nombre de una pelicula y el auxiliar el anio anterior
		
		try{
			Document doc = buscar_arbol_por_anio(auxiliar); // busco el arbol del anio que se indico antes (guardado posteriormente en auxiliar)
			enviar_cabecera_html();
			enviar_cuerpo_html();
			
			NodeList info_pelicula = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula[Titulo='"+peliculas.elementAt(Integer.parseInt(variable))+"']/Titulo/text()",doc,XPathConstants.NODESET));
			out.println("<H2><U> INFORMACION DE LA PELICULA: " + info_pelicula.item(0).getNodeValue()+ "</U></H2><p><p>");
			
			
			NodeList info_pelicula_2 = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula[Titulo='"+peliculas.elementAt(Integer.parseInt(variable))+"']",doc,XPathConstants.NODESET));
			out.println("IDENTIFICADOR: " + info_pelicula_2.item(0).getAttributes().item(0).getNodeValue() + "<p> ");
			out.println("IDIOMAS: " + info_pelicula_2.item(0).getAttributes().item(1).getNodeValue() + "<p>");
			
			out.println("<H3>PAIS:</H3> ");
			NodeList info_pelicula_3 = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula[Titulo='"+peliculas.elementAt(Integer.parseInt(variable))+"']/Pais/text()",doc,XPathConstants.NODESET));
			out.println(info_pelicula_3.item(0).getNodeValue()+ "<p><p>");
			
			out.println("<H3>GENERO/S:</H3> ");
			NodeList info_pelicula_4 = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula[Titulo='"+peliculas.elementAt(Integer.parseInt(variable))+"']/Generos/Genero/text()",doc,XPathConstants.NODESET));
			for (int i = 0; i < info_pelicula_4.getLength(); i++) {
				if (i!=(info_pelicula_4.getLength()-1)) {out.println(info_pelicula_4.item(i).getNodeValue()+ " , ");}
				else {out.println(info_pelicula_4.item(i).getNodeValue());}
			}
			out.println("<p>");
			
			out.println("<H3>OSCAR/ES:</H3> ");
			NodeList info_pelicula_5 = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula[Titulo='"+peliculas.elementAt(Integer.parseInt(variable))+"']/Oscars/Oscar/text()",doc,XPathConstants.NODESET));
			if (info_pelicula_5.getLength()!=0) {
				for (int i = 0; i < info_pelicula_5.getLength(); i++) {
					if (i!=(info_pelicula_5.getLength()-1)) {out.println(info_pelicula_5.item(i).getNodeValue()+ " , ");}
					else {out.println(info_pelicula_5.item(i).getNodeValue());}
				}
			}	 else {out.println("No hay informacion de oscares");}
			out.println("<p>");
			
			out.println("<H3>DIRECTOR:</H3> ");
			NodeList info_pelicula_6 = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula[Titulo='"+peliculas.elementAt(Integer.parseInt(variable))+"']/Director/Nombre/text()",doc,XPathConstants.NODESET));
			out.println(info_pelicula_6.item(0).getNodeValue());
			
			out.println("<H3>ACTOR/ES:</H3> ");
			NodeList info_pelicula_7 = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula[Titulo='"+peliculas.elementAt(Integer.parseInt(variable))+"']/Actor/Nombre/text()",doc,XPathConstants.NODESET));
			if (info_pelicula_7.getLength()!=0) {
				for (int i = 0; i < info_pelicula_7.getLength(); i++) {
					if (i!=(info_pelicula_7.getLength()-1)) {
						out.println(info_pelicula_7.item(i).getNodeValue());
					}else {
						out.println(info_pelicula_7.item(i).getNodeValue());
					}
				}
			}else {out.println("No hay informacion de actores");}
			out.println("<p>");
			
			enviar_paginas_anteriores();
			enviar_final_html("1","4",variable);
		}catch (XPathExpressionException xee) {
			System.err.println(xee);
			System.exit(1);
		}
	}
	
	
	/*****************************************************************************************/
	/************************************* CONSULTA 2 ****************************************/
	/*****************************************************************************************/
	
	
	public void clasificacion_oscars(){
		switch (Integer.parseInt(paso)) {
			case 1:
				anios_disponibles(1);
				break;
			case 2:
				clasificacion_peliculas_del_anio_indicado();
				break;
			default:
				break;
		}
	}
	
	public void clasificacion_peliculas_del_anio_indicado(){ //puede ser toda la historia (9999)
		
		num_oscars = new Vector<String>();
		int contador=0;
		try{ 
			
			enviar_cabecera_html();
			enviar_cuerpo_html();
			
			out.println("<H2><U>TOP 5 PELICULAS</U></H2><p><p>");
			if (Integer.parseInt(variable)!=9999){ // Busco las de un anio
				
				Document doc = buscar_arbol_por_anio(variable);
				NodeList nodos_oscars = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula/Oscars[@cantidad]",doc,XPathConstants.NODESET));
				for (int j=0; j < nodos_oscars.getLength(); j++){
					if (num_oscars.contains(nodos_oscars.item(j).getAttributes().item(0).getNodeValue())==false){
						num_oscars.addElement(nodos_oscars.item(j).getAttributes().item(0).getNodeValue());
					}
				}
				
				Collections.sort(num_oscars); // los ordena de MENOR a MAYOR
				
				for (int i=(num_oscars.size()-1); i > -1; i--){
					
					NodeList nodos_oscars_titulo = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula/Oscars[@cantidad='" + num_oscars.elementAt(i) +"']/parent::*/Titulo/text()",doc,XPathConstants.NODESET));
					for (int j = 0; j < nodos_oscars_titulo.getLength(); j++){
						if (contador<5){
							out.println(nodos_oscars_titulo.item(j).getNodeValue() + " (" + num_oscars.elementAt(i) + ")<p>");
							contador++;
						}
					}
					
				}
			}else { // Toda la historia
				for (int i=0; i<arboles.size(); i++) { // Busco todas las cantidades
					NodeList nodos_oscars_historia = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula/Oscars[@cantidad]",arboles.elementAt(i),XPathConstants.NODESET));
					for (int j=0; j < nodos_oscars_historia.getLength(); j++){
						if (num_oscars.contains(nodos_oscars_historia.item(j).getAttributes().item(0).getNodeValue())==false){
							num_oscars.addElement(nodos_oscars_historia.item(j).getAttributes().item(0).getNodeValue());
						}
					}
				}
				
				Collections.sort(num_oscars);
				
				for (int i=(num_oscars.size()-1); i > -1; i--){ // Busco cada cantidad en todos los arboles
					
					for (int j=0; j<arboles.size(); j++) { 
						NodeList nodos_oscars_titulo = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula/Oscars[@cantidad='" + num_oscars.elementAt(i) +"']/parent::*/Titulo/text()",arboles.elementAt(j),XPathConstants.NODESET));
						for (int k = 0; k < nodos_oscars_titulo.getLength(); k++){
							if (contador<5){
								out.println(nodos_oscars_titulo.item(k).getNodeValue() + " (" + num_oscars.elementAt(i) + ")<p>");
								contador++;
							}
						}
					}
				}
			}
		}catch (XPathExpressionException xee) {
			System.err.println(xee);
			System.exit(1);
		}
		enviar_paginas_anteriores();
		enviar_final_html("2","3",variable);
	}
	
	
	/*****************************************************************************************/
	/************************************* CONSULTA 3 ****************************************/
	/*****************************************************************************************/
	
	
	public void pelicula_pais_anio(){
		switch (Integer.parseInt(paso)) {
			case 1:
				paises_disponibles();
				break;
			case 2:
				anios_disponibles_del_pais_indicado();
				break;
			case 3:
				peliculas_del_pais_anio_indicado();
				break;
			default:
				break;
		}
		
	}
	
	public void paises_disponibles(){
		
		paises_leidos = new Vector<String>();
		
		try{
			enviar_cabecera_html();
			enviar_cuerpo_html();
			
			for (int i=0; i<arboles.size(); i++) {
				
				NodeList nodos_paises = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pais/text()",arboles.elementAt(i),XPathConstants.NODESET));
				
				for (int j=0; j < nodos_paises.getLength(); j++) { //el for es un poco estupido porque solo puede haber un año
					String pais = nodos_paises.item(j).getNodeValue();
					if ((paises_leidos.contains(pais))==false){
						paises_leidos.addElement(pais);
						enviar_boton(Integer.toString(paises_leidos.indexOf(pais)),nodos_paises.item(j).getNodeValue());
					}
				}
			}
			
			enviar_boton_submit();
			enviar_final_html("3","2","0"); //en la consulta 3, el siguiente paso es enviar el año del pais que llegan
			//Aqui da igual el auxiliar que se le ponga
		}catch (XPathExpressionException xee) {
			System.err.println(xee);
			System.exit(1);
		}
	}
	
	public void anios_disponibles_del_pais_indicado(){
		
		try {
			
			enviar_cabecera_html();
			enviar_cuerpo_html();
			
			for (int i = 0; i<arboles.size(); i++){
				NodeList nodo_peliculas_pais = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula[Pais='"+paises_leidos.elementAt(Integer.parseInt(variable))+"']/Titulo/text()",arboles.elementAt(i),XPathConstants.NODESET));
				if (nodo_peliculas_pais.getLength()!=0){
					NodeList nodo_anio_pais = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Cine/Anio/text()",arboles.elementAt(i),XPathConstants.NODESET));
					for (int j=0; j < nodo_anio_pais.getLength(); j++) { //el for es un poco estupido porque solo puede haber un año
						enviar_boton(nodo_anio_pais.item(j).getNodeValue(),nodo_anio_pais.item(j).getNodeValue());
					}
				}
			}
			enviar_boton_submit();
			enviar_final_html("3","3",variable);//en la consulta 3, el siguiente paso es enviar el año del pais que llegan, la variable es el pais correspondiente				 
			
		}catch (XPathExpressionException xee) {
			System.err.println(xee);
			System.exit(1);
		}
	}
	
	public void peliculas_del_pais_anio_indicado(){
		
		
		idiomas_leidos = new Vector<String>();
		peliculas_leidas = new Vector<String>();
		
		int primera_vez=0;
		int indice_idiomas=0;
		
		try{
			
			enviar_cabecera_html();
			enviar_cuerpo_html();
			
			Document doc = buscar_arbol_por_anio(variable);
			NodeList nodo_peliculas_pais_anio = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula[Pais='"+paises_leidos.elementAt(Integer.parseInt(auxiliar))+"']/Titulo/text()",doc,XPathConstants.NODESET));
			out.println("<H2>PELICULAS POR IDIOMA ORIGINAL</H2><p><p>");
			for (int i=0; i < nodo_peliculas_pais_anio.getLength(); i++){
				
				NodeList nodo_langs_peliculas_pais_anio = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula[Titulo='"+nodo_peliculas_pais_anio.item(i).getNodeValue()+"']",doc,XPathConstants.NODESET));
				
				//saco el atributo idiomas, concateno las 2 primeras letras y la meto en idiomas leidos.
				String idiomas = nodo_langs_peliculas_pais_anio.item(0).getAttributes().item(1).getNodeValue();
				String idioma = new StringBuilder().append(idiomas.charAt(0)).append(idiomas.charAt(1)).toString();
				
				if (idiomas_leidos.contains(idioma)==false) {
					idiomas_leidos.addElement(idioma);
					out.println("<H2><U>" + idioma + "</U></H2><p>");
					
					NodeList nodo_peliculas_pais_anio_2 = nodo_peliculas_pais_anio; //(NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula[Pais='"+paises_leidos.elementAt(Integer.parseInt(auxiliar))+"']/Titulo/text()",doc,XPathConstants.NODESET));
					for (int j=i; j< nodo_peliculas_pais_anio_2.getLength(); j++){
						
						NodeList nodo_langs_peliculas_pais_anio_2 = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula[Titulo='"+nodo_peliculas_pais_anio_2.item(j).getNodeValue()+"']",doc,XPathConstants.NODESET));
						String idiomas_actual = nodo_langs_peliculas_pais_anio_2.item(0).getAttributes().item(1).getNodeValue();
						String idioma_actual = new StringBuilder().append(idiomas_actual.charAt(0)).append(idiomas_actual.charAt(1)).toString();
						
						//out.println("para la pelicula: "+ nodo_peliculas_pais_anio_2.item(j).getNodeValue() + 
						if ( idiomas_leidos.elementAt(indice_idiomas).equals(idioma_actual)){
							
							if (peliculas_leidas.contains(nodo_peliculas_pais_anio_2.item(j).getNodeValue())==false) { // si la pelicula todavia no esta añadida
								peliculas_leidas.addElement(nodo_peliculas_pais_anio_2.item(j).getNodeValue());
								out.println(nodo_peliculas_pais_anio_2.item(j).getNodeValue() + "<p>");
							}
						}
						
					}
					indice_idiomas++;
					out.println("<p>");
				}
			}
			
			enviar_paginas_anteriores();
			enviar_final_html("3","4",variable);
			
		}catch (XPathExpressionException xee) {
			System.err.println(xee);
			System.exit(1);
		}
	}
	
	
	/*****************************************************************************************/
	/************************************* CONSULTA 4 ****************************************/
	/*****************************************************************************************/
	
	
	public void resumen_actor_director(){
		switch (Integer.parseInt(paso)) {
			case 1:
				actores_directores_disponibles();
				break;
			case 2:
				info_actor_director();
				break;
			default:
				break;
		}
	}
	
	public void actores_directores_disponibles(){
		
		actores_directores_leidos = new Vector<String>();
		
		
		try{
			enviar_cabecera_html();
			enviar_cuerpo_html();
			
			for (int i=0; i<arboles.size(); i++) {
				
				//TODOS LOS ACTORES DE UN DOM
				NodeList nodos_actores = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula/Actor/Nombre/text()",arboles.elementAt(i),XPathConstants.NODESET));
				
				for (int j=0; j < nodos_actores.getLength(); j++) { //el for es un poco estupido porque solo puede haber un año
					String actor = nodos_actores.item(j).getNodeValue();
					if ((actores_directores_leidos.contains(actor))==false){
						actores_directores_leidos.addElement(actor);
						enviar_boton(Integer.toString(actores_directores_leidos.indexOf(nodos_actores.item(j).getNodeValue())),nodos_actores.item(j).getNodeValue());
					}
				}
				
				//TODOS LOS DIRECTORES DE UN DOM
				NodeList nodos_directores = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula/Director/Nombre/text()",arboles.elementAt(i),XPathConstants.NODESET));
				
				for (int j=0; j < nodos_directores.getLength(); j++) { //el for es un poco estupido porque solo puede haber un año
					String director = nodos_directores.item(j).getNodeValue();
					if ((actores_directores_leidos.contains(director))==false){
						actores_directores_leidos.addElement(director);
						enviar_boton(Integer.toString(actores_directores_leidos.indexOf(nodos_directores.item(j).getNodeValue())),nodos_directores.item(j).getNodeValue());
					}
				}
			}
			
			enviar_boton_submit();
			enviar_final_html("4","2","0"); //en la consulta 4, el siguiente paso es mostrar toda la informacion
			//Aqui da igual el auxiliar que se le ponga
		}catch (XPathExpressionException xee) {
			System.err.println(xee);
			System.exit(1);
		}
	}
	
	public void info_actor_director(){
		
		int contador=0;
		
		try{
			
			
			enviar_cabecera_html();
			enviar_cuerpo_html();
			for (int i=0; i<arboles.size(); i++) {
				NodeList nodo_actor_director = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Pelicula/Director[Nombre='" + actores_directores_leidos.elementAt(Integer.parseInt(variable)) + "']/text() | //Pelicula/Actor[Nombre='" + actores_directores_leidos.elementAt(Integer.parseInt(variable)) + "']/text()",arboles.elementAt(i),XPathConstants.NODESET));
				if (nodo_actor_director.getLength()!=0){
					if (contador==0){
						out.println("<H2><U>INFORMACION DEL DIRECTOR O ACTOR: "+ actores_directores_leidos.elementAt(Integer.parseInt(variable)) +"</U></H2>");
						out.println(nodo_actor_director.item(1).getNodeValue()+ "<p>");
						contador++;
					}
				}
			}
			
			enviar_paginas_anteriores();
			enviar_final_html("4","3",variable);
			
		}catch (XPathExpressionException xee) {
			System.err.println(xee);
			System.exit(1);
		}
	}
	
	
	/*****************************************************************************************/
	/**************************************** INIT *******************************************/
	/*****************************************************************************************/
	
	
	public void parsear_xml(){
		
		try{
						
			while (xml_faltan.isEmpty()==false) {
				//Crear DocumentBuildFactory y configurarlo
				DocumentBuilderFactory domFactory = DocumentBuilderFactory.newInstance();
				domFactory.setValidating(true); //Activa la validacion del xml
				domFactory.setNamespaceAware(true);
				
				
				//Crear un DocumentBuilder que satisfaga la configuracion anterior.
				DocumentBuilder builder = domFactory.newDocumentBuilder();
				
				builder.setErrorHandler(new MyErrorHandler());
				
				doc = builder.parse(xml_faltan.firstElement());
				arboles.addElement(doc);
				
				xml_leidos.addElement(xml_faltan.firstElement());
				xml_faltan.remove(0);
				
				NodeList nodo_nuevos_ficheros = (NodeList)(XPathFactory.newInstance().newXPath().evaluate("//Otra_Pelicula/Resumen/text()",doc,XPathConstants.NODESET));
				
				//Por cada nueva linea de Resumen (supuesto nuevo xml)se busca si ya esta parseado
				for (int i=0; i<nodo_nuevos_ficheros.getLength(); i++) {
					String dir_fichero = nodo_nuevos_ficheros.item(i).getNodeValue();
					for (int j=0; j<xml_leidos.size(); j++) {
						if ((xml_leidos.contains(dir_fichero)==false) && (xml_faltan.contains(dir_fichero)==false)) {
							xml_faltan.addElement(dir_fichero);
						}
					}
				}
			}
		}catch(FileNotFoundException fe){
			problema.addElement("Archivo no encontrado: " + xml_faltan.firstElement() + " (FileNotFoundException)");
			xml_leidos.addElement(xml_faltan.firstElement());
			xml_faltan.remove(0);
			parsear_xml();
		}catch (ParserConfigurationException pce) {
			problema.addElement(pce.getMessage());
			problema.addElement(xml_faltan.firstElement());
			xml_leidos.addElement(xml_faltan.firstElement());
			xml_faltan.remove(0);
			parsear_xml();
			//System.err.println(pce);
			//System.exit(1);
		}catch (SAXException se){
			problema.addElement("Problema al parsear: " + xml_faltan.firstElement()+ " (SAXException)");
			xml_leidos.addElement(xml_faltan.firstElement());
			xml_faltan.remove(0);
			parsear_xml();
		}catch (IOException e) {
			problema.addElement(e.getMessage());
			problema.addElement(xml_faltan.firstElement());
			xml_leidos.addElement(xml_faltan.firstElement());
			xml_faltan.remove(0);
			parsear_xml();
		}catch (XPathExpressionException xee) {
			problema.addElement(xee.getMessage());
			problema.addElement(xml_faltan.firstElement());
			xml_leidos.addElement(xml_faltan.firstElement());
			xml_faltan.remove(0);
			parsear_xml();
			//System.err.println(xee);
			//System.exit(1);
		}
	}
	
	
	/*****************************************************************************************/
	/************************************ IMPRIMIR HTML **************************************/
	/*****************************************************************************************/
	
	
	public void enviar_cabecera_html(){

		out.println("<!DOCTYPE HTML PUBLIC '-//W3C//DTD HTML 4.0 Transitional//EN' 'http://www.w3.org/TR/html4/strict.dtd'>");
		out.println("<HTML>");
		out.println("<HEAD><TITLE>PRACTICA 2</TITLE>");
		out.println("<LINK rel='stylesheet' href='p2/index.css'>"); 
		out.println("</HEAD>");	
		out.println("<SCRIPT type='text/javascript'>");
		out.println("function ejecutar(){");
		out.println("document.formulario.action ='http://localhost:8080/lro3/servlet';");
		out.println("return true;}");
		out.println("</SCRIPT>");
	}
	
	public void enviar_cuerpo_html(){
		out.println("<BODY>");
		out.println("<CENTER>");
		out.println("<H1>SERVICIO DE CONSULTAS</H1>");
		if (problema.size()!=0)	{
			for (int i=0 ; i<problema.size(); i++){
				out.println("Problema con: " + problema.elementAt(i) + "<p>");
			}
		}
		
		out.println("<FORM name='formulario' action='' method='get' onSubmit='return ejecutar();'>");
	}
	
	public void enviar_boton(String valor,String pantalla){
		out.println("<INPUT type='radio' Name='variable' Value=" + valor + "> " + pantalla + "<p>");
	}
	
	public void enviar_boton_submit(){
		out.println("<INPUT type='Submit' value= 'Submit'><p>");
		out.println("<a href='javascript:window.history.back();'><font color='green'>Pagina anterior</a></font><p>");
		out.println("<a href=p2/index.html><font color='red'>Pagina inicial</a></font><p>");
	}
	
	public void enviar_paginas_anteriores(){
		out.println("<a href='javascript:window.history.back();'><font color='green'>Pagina anterior</a></font><p>");
		out.println("<a href=p2/index.html><font color='red'>Pagina inicial</a></font><p>");
	}
	
	public void enviar_final_html(String consulta,String paso,String auxiliar){
		out.println("<INPUT type='hidden' name='consulta' Value=" + consulta + ">");
		out.println("<INPUT type='hidden' name='paso' Value=" + paso + ">");
		out.println("<INPUT type='hidden' name='auxiliar' Value=" + auxiliar + ">");
		out.println("</FORM>");
		out.println("</CENTER>");
		out.println("</BODY>");
		out.println("</HTML>");
	}
	
	
	public class MyErrorHandler implements ErrorHandler {
		public void warning(SAXParseException e) throws SAXException {
			problema.addElement(e.getMessage());
			throw new SAXException(e);
    		}
		
		public void error(SAXParseException e) throws SAXException {
			problema.addElement(e.getMessage());
			throw new SAXException(e);
		}
		
		public void fatalError(SAXParseException e) throws SAXException {
			problema.addElement(e.getMessage());
			throw new SAXException(e);
		}	
		
	}
}
	
	
	
