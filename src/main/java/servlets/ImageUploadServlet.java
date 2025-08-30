package servlets;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

@WebServlet("/upload")
@MultipartConfig(
    maxFileSize = 5 * 1024 * 1024,      
    maxRequestSize = 10 * 1024 * 1024,  
    fileSizeThreshold = 1024 * 1024     
)
public class ImageUploadServlet extends HttpServlet {
    
    private static final String UPLOAD_DIR = "uploads";
    
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        
        
        String applicationPath = request.getServletContext().getRealPath("");
        String uploadPath = applicationPath + File.separator + UPLOAD_DIR;
        File uploadDir = new File(uploadPath);
        if (!uploadDir.exists()) {
            uploadDir.mkdirs();
        }
        
        try {
            
            Part filePart = request.getPart("image");
            String fileName = getFileName(filePart);
            
            if (fileName != null && !fileName.isEmpty()) {
                
                String contentType = filePart.getContentType();
                if (!isValidImageType(contentType)) {
                    response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("Nevaljan tip fajla");
                    return;
                }
                
                
                String uniqueFileName = generateUniqueFileName(fileName);
                String filePath = uploadPath + File.separator + uniqueFileName;
                
                
                filePart.write(filePath);
                
                
                response.setContentType("application/json");
                response.getWriter().write("{\"success\": true, \"fileName\": \"" + uniqueFileName + "\"}");
                
            } else {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write("Fajl nije poslan");
            }
            
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Greška pri upload-u: " + e.getMessage());
        }
    }
    
    private String getFileName(Part part) {
        String contentDisposition = part.getHeader("content-disposition");
        for (String token : contentDisposition.split(";")) {
            if (token.trim().startsWith("filename")) {
                return token.substring(token.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return null;
    }
    
    private boolean isValidImageType(String contentType) {
        return contentType != null && 
               (contentType.equals("image/jpeg") || 
                contentType.equals("image/jpg") || 
                contentType.equals("image/png") || 
                contentType.equals("image/gif"));
    }
    
    private String generateUniqueFileName(String originalFileName) {
        String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
        return System.currentTimeMillis() + "_" + UUID.randomUUID().toString() + extension;
    }
}