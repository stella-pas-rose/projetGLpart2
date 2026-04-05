package us.codecraft.webmagic.pipeline;
import com.alibaba.fastjson.JSON;

/**
 * Store results objects (page models) to files in JSON format.<br>
 * Use model.getKey() as file name if the model implements HasKey.<br>
 * Otherwise use SHA1 as file name.
 *
 * @author code4crafter@gmail.com <br>
 * @since 0.2.0
 */
public class JsonFilePageModelPipeline extends ToolsFilePageModelPipeline{

    /**
     * new JsonFilePageModelPipeline with default path "/data/webmagic/"
     */
    public JsonFilePageModelPipeline() {
        super();
    }

    public JsonFilePageModelPipeline(String path) {
        super(path);
    }

    protected String getExtension(){
        return ".json"; 
    }

    protected String transform(Object o){
        return JSON.toJSONString(o);
    }

}
