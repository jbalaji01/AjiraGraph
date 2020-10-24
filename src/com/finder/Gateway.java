package com.finder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.finder.json.Json;

/**
 * Servlet implementation class Gateway
 */
@WebServlet("/process")
public class Gateway extends HttpServlet {
	private static final long serialVersionUID = 1L;

    /**
     * Default constructor. 
     */
    public Gateway() {
        
    }
    

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		int returnCode = HttpServletResponse.SC_OK;
		String msg = "";
		
		try {
			
			String line;
			List<String> inputList = new ArrayList<>();
			BufferedReader reader = request.getReader();
		    while ((line = reader.readLine()) != null)
		      inputList.add(line);
			
		    GraphService graphService = new GraphService();
		    msg = graphService.processCommand(inputList);
		    
			
		}catch (BadDataException bde) {
			returnCode = bde.getErrorCode();
			msg = Json.composeMsg(returnCode, bde.getMessage());
		}
		
		response.setStatus(returnCode);
		PrintWriter pw=response.getWriter();    
		pw.println(msg);  
		pw.close(); 
	}


	

}
