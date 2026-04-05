package us.codecraft.webmagic.pipeline;
import org.apache.commons.lang3.builder.ToStringBuilder;


/**
 * Store results objects (page models) to files in plain format.<br>
 * Use model.getKey() as file name if the model implements HasKey.<br>
 * Otherwise use SHA1 as file name.
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.3.0
 */
public class FilePageModelPipeline extends ToolsFilePageModelPipeline {

    /**
     * new JsonFilePageModelPipeline with default path "/data/webmagic/"
     */
    public FilePageModelPipeline() {
        super();
    }

    public FilePageModelPipeline(String path) {
        super(path);
    }

    protected String getExtension(){
        return ".html"; 
    }

    protected String transform(Object o){
        return ToStringBuilder.reflectionToString(o); 
    }

}
