package us.codecraft.webmagic.pipeline;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import us.codecraft.webmagic.Task;
import us.codecraft.webmagic.model.HasKey;
import us.codecraft.webmagic.utils.FilePersistentBase;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public abstract class ToolsFilePageModelPipeline extends FilePersistentBase implements PageModelPipeline<Object> {
    protected Logger logger= LoggerFactory.getLogger(getClass());
    
    public ToolsFilePageModelPipeline(){
        setPath("/data/webmagic");
    }

    public ToolsFilePageModelPipeline(String path){
        setPath(path);
    }

    @Override
    public void process(Object o, Task task) {
        String path = this.path + PATH_SEPERATOR + task.getUUID() + PATH_SEPERATOR;
        try {
            String filename;
            if (o instanceof HasKey) {
                filename = path + ((HasKey) o).key() + getExtension() ;
            } else {
                filename = path + DigestUtils.md5Hex(ToStringBuilder.reflectionToString(o)) + getExtension();
            }
            PrintWriter printWriter = new PrintWriter(new FileWriter(getFile(filename)));
            printWriter.write(transform(o));
            printWriter.close();
        } catch (IOException e) {
            logger.warn("write file error", e);
        }
    }

    /**
     * return the extension for the specific type of file
     * @return a string of the extension
     */
    protected abstract String getExtension(); 


    /**
     * transform the given object in a string
     * @param o the object to trnaform 
     * @return a string representing an object
     */
    protected abstract String transform(Object o); 

}
