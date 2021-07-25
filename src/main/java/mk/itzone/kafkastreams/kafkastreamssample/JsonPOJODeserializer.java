package mk.itzone.kafkastreams.kafkastreamssample;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.errors.SerializationException;
import org.apache.kafka.common.serialization.Deserializer;

import java.util.Map;

public class JsonPOJODeserializer<T> implements Deserializer<T> {
    private ObjectMapper objectMapper = new ObjectMapper();

    private Class<T> tClass;

    /**
     * Default constructor needed by Kafka
     */
    public JsonPOJODeserializer() {
    }

    @SuppressWarnings("unchecked")
    @Override
    public void configure(Map<String, ?> props, boolean isKey) {
        tClass = (Class<T>) props.get("JsonPOJOClass");
    }

    @Override
    public T deserialize(String topic, byte[] bytes) {

//        System.out.println(">>>>>>>>>>>>>>>>>>>deserialize");

        if (bytes == null)
            return null;

        T data;
        try {
//            System.out.println(">>>>>>>>>>>>>>>>>>>try "+ tClass.getName());
            data = objectMapper.readValue(bytes, tClass);
        } catch (Exception e) {
//            System.out.println(">>>>>>>>>>>>>>>>>>>exception");
            throw new SerializationException(e);
        }

        return data;
    }

    @Override
    public void close() {

    }
}