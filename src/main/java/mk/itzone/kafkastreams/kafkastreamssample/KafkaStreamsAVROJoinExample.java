package mk.itzone.kafkastreams.kafkastreamssample;


//import io.confluent.common.utils.TestUtils;
//import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import com.fasterxml.jackson.annotation.JsonIgnore;
//import io.confluent.kafka.serializers.AbstractKafkaAvroSerDeConfig;
//import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
import mk.itzone.kafkastreams.avro.Movie;
import mk.itzone.kafkastreams.avro.RatedMovie;
import mk.itzone.kafkastreams.avro.Rating;
import mk.itzone.kafkastreams.avro.TicketSale;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.SslConfigs;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.kstream.*;

//import mk.itzone.kafkastreams.avro.TicketSale;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Pattern;
//import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;

/**
 * Demonstrates, using the high-level KStream DSL, how to implement the WordCount program that
 * computes a simple word occurrence histogram from an input text. This example uses lambda
 * expressions and thus works with Java 8+ only.
 * <p>
 * In this example, the input stream reads from a topic named "streams-plaintext-input", where the values of
 * messages represent lines of text; and the histogram output is written to topic
 * "streams-wordcount-output", where each record is an updated count of a single word, i.e. {@code word (String) -> currentCount (Long)}.
 * <p>
 * Note: Before running this example you must 1) create the source topic (e.g. via {@code kafka-topics --create ...}),
 * then 2) start this example and 3) write some data to the source topic (e.g. via {@code kafka-console-producer}).
 * Otherwise you won't see any data arriving in the output topic.
 * <p>
 * <br>
 * HOW TO RUN THIS EXAMPLE
 * <p>
 * 1) Start Zookeeper and Kafka. Please refer to <a href='http://docs.confluent.io/current/quickstart.html#quickstart'>QuickStart</a>.
 * <p>
 * 2) Create the input and output topics used by this example.
 * <pre>
 * {@code
 * $ bin/kafka-topics --bootstrap-server localhost:9092 --create --topic streams-plaintext-input \
 *                   --partitions 1 --replication-factor 1
 * $ bin/kafka-topics --bootstrap-server localhost:9092 --create --topic streams-wordcount-output \
 *                   --partitions 1 --replication-factor 1
 * }</pre>
 * Note: The above commands are for the Confluent Platform. For Apache Kafka it should be {@code bin/kafka-topics.sh ...}.
 * <p>
 * 3) Start this example application either in your IDE or on the command line.
 * <p>
 * If via the command line please refer to <a href='https://github.com/confluentinc/kafka-streams-examples#packaging-and-running'>Packaging</a>.
 * Once packaged you can then run:
 * <pre>
 * {@code
 * $ java -cp target/kafka-streams-examples-6.2.0-standalone.jar io.confluent.examples.streams.WordCountLambdaAVROExample
 * }
 * </pre>
 * 4) Write some input data to the source topic "streams-plaintext-input" (e.g. via {@code kafka-console-producer}).
 * The already running example application (step 3) will automatically process this input data and write the
 * results to the output topic "streams-wordcount-output".
 * <pre>
 * {@code
 * # Start the console producer. You can then enter input data by writing some line of text, followed by ENTER:
 * #
 * #   hello kafka streams<ENTER>
 * #   all streams lead to kafka<ENTER>
 * #   join kafka summit<ENTER>
 * #
 * # Every line you enter will become the value of a single Kafka message.
 * $ bin/kafka-console-producer --broker-list localhost:9092 --topic streams-plaintext-input
 * }</pre>
 * 5) Inspect the resulting data in the output topic, e.g. via {@code kafka-console-consumer}.
 * <pre>
 * {@code
 * $ bin/kafka-console-consumer --topic streams-wordcount-output --from-beginning \
 *                              --bootstrap-server localhost:9092 \
 *                              --property print.key=true \
 *                              --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer
 * }</pre>
 * You should see output data similar to below. Please note that the exact output
 * sequence will depend on how fast you type the above sentences. If you type them
 * slowly, you are likely to get each count update, e.g., kafka 1, kafka 2, kafka 3.
 * If you type them quickly, you are likely to get fewer count updates, e.g., just kafka 3.
 * This is because the commit interval is set to 10 seconds. Anything typed within
 * that interval will be compacted in memory.
 * <pre>
 * {@code
 * hello    1
 * kafka    1
 * streams  1
 * all      1
 * streams  2
 * lead     1
 * to       1
 * join     1
 * kafka    3
 * summit   1
 * }</pre>
 * 6) Once you're done with your experiments, you can stop this example via {@code Ctrl-C}. If needed,
 * also stop the Kafka broker ({@code Ctrl-C}), and only then stop the ZooKeeper instance (`{@code Ctrl-C}).
 */
public class KafkaStreamsAVROJoinExample {

  abstract class IgnoreSchemaProperty
  {
    // You have to use the correct package for JsonIgnore,
    // fasterxml or codehaus
    @JsonIgnore
    abstract void getSchema();
  }

  // mkin2
  static final String movieTopic = "movieTopic";
  static final String rekeyedMovieTopic = "rekeyedMovieTopic";
  static final String ratedMoviesTopic = "ratedMoviesTopic";
  static final String ratingTopic = "ratingTopic";
  static final String outputTopic = "mkout2";

  /*

ssh centos@kafka-1.non-prod.cloud.corp.stokrotka.pl "sudo /usr/local/kafka/bin/kafka-topics.sh --bootstrap-server kafka-1.non-prod.cloud.corp.stokrotka.pl:9093 --command-config /usr/local/kafka/config/admin.properties --create replication-factor 2 partitions 6 --topic movieTopic" && \
ssh centos@kafka-1.non-prod.cloud.corp.stokrotka.pl "sudo /usr/local/kafka/bin/kafka-topics.sh --bootstrap-server kafka-1.non-prod.cloud.corp.stokrotka.pl:9093 --command-config /usr/local/kafka/config/admin.properties --create replication-factor 2 partitions 6 --topic rekeyedMovieTopic" && \
ssh centos@kafka-1.non-prod.cloud.corp.stokrotka.pl "sudo /usr/local/kafka/bin/kafka-topics.sh --bootstrap-server kafka-1.non-prod.cloud.corp.stokrotka.pl:9093 --command-config /usr/local/kafka/config/admin.properties --create replication-factor 2 partitions 6 --topic ratedMoviesTopic" && \
ssh centos@kafka-1.non-prod.cloud.corp.stokrotka.pl "sudo /usr/local/kafka/bin/kafka-topics.sh --bootstrap-server kafka-1.non-prod.cloud.corp.stokrotka.pl:9093 --command-config /usr/local/kafka/config/admin.properties --create replication-factor 2 partitions 6 --topic ratingTopic" && \
ssh centos@kafka-1.non-prod.cloud.corp.stokrotka.pl "sudo /usr/local/kafka/bin/kafka-topics.sh --bootstrap-server kafka-1.non-prod.cloud.corp.stokrotka.pl:9093 --command-config /usr/local/kafka/config/admin.properties --create replication-factor 2 partitions 6 --topic mkout2"

ssh centos@kafka-1.non-prod.cloud.corp.stokrotka.pl "sudo /usr/local/kafka/bin/kafka-topics.sh --bootstrap-server kafka-1.non-prod.cloud.corp.stokrotka.pl:9093 --command-config /usr/local/kafka/config/admin.properties --list"


#ssh centos@kafka-1.non-prod.cloud.corp.stokrotka.pl "sudo  /usr/local/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka-1.non-prod.cloud.corp.stokrotka.pl:9093 --producer.config /usr/local/kafka/config/admin.properties --property key.schema='{"type":"string"}' --property value.schema="$(< /opt/app/schema/movie.avsc) --topic movieTopic"

ssh centos@kafka-1.non-prod.cloud.corp.stokrotka.pl "sudo  /usr/local/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka-1.non-prod.cloud.corp.stokrotka.pl:9093 --producer.config /usr/local/kafka/config/admin.properties --topic movieTopic"


MOVIES

{"id": 294, "title": "Die Hard", "releaseyear": 1988}
{"id": 354, "title": "Tree of Life", "releaseyear": 2011}
{"id": 782, "title": "A Walk in the Clouds", "releaseyear": 1995}
{"id": 128, "title": "The Big Lebowski", "releaseyear": 1998}
{"id": 780, "title": "Super Mario Bros.", "releaseyear": 1993}


ssh centos@kafka-1.non-prod.cloud.corp.stokrotka.pl "sudo  /usr/local/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka-1.non-prod.cloud.corp.stokrotka.pl:9093 --producer.config /usr/local/kafka/config/admin.properties --topic mkin"


ssh centos@kafka-1.non-prod.cloud.corp.stokrotka.pl "echo '{"title":"Die Hard","salets":"2019-07-18T10:00:00Z","tickettotalvalue":12}' | sudo  /usr/local/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka-1.non-prod.cloud.corp.stokrotka.pl:9093 --producer.config /usr/local/kafka/config/admin.properties --topic mkin"
ssh centos@kafka-1.non-prod.cloud.corp.stokrotka.pl "echo '{"title":"Die Hard","salets":"2019-07-18T10:01:00Z","tickettotalvalue":12}' | sudo  /usr/local/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka-1.non-prod.cloud.corp.stokrotka.pl:9093 --producer.config /usr/local/kafka/config/admin.properties --topic mkin"


ssh centos@kafka-1.non-prod.cloud.corp.stokrotka.pl "sudo /usr/local/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka-1.non-prod.cloud.corp.stokrotka.pl:9093 --consumer.config /usr/local/kafka/config/admin.properties --from-beginning --property print.key=true --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer --topic mkout"

mvn clean generate-sources install -DskipTests && java -jar target/kafkastreamsample-0.0.1-SNAPSHOT-jar-with-dependencies.jar

mvn clean generate-sources install -DskipTests && java -cp target/kafkastreamsample-0.0.1-SNAPSHOT-jar-with-dependencies.jar mk.itzone.kafkastreams.kafkastreamssample.WordCountLambdaAVROExample


   */
  public static void main(final String[] args) {
    final String bootstrapServers = args.length > 0 ? args[0] : "kafka-1.non-prod.cloud.corp.stokrotka.pl:9093";

    // Configure the Streams application.
    final Properties streamsConfiguration = getStreamsConfiguration(bootstrapServers);

    // Define the processing topology of the Streams application.
    final StreamsBuilder builder = new StreamsBuilder();
    createWordCountStream(streamsConfiguration,builder);
    final KafkaStreams streams = new KafkaStreams(builder.build(), streamsConfiguration);

    // Always (and unconditionally) clean local state prior to starting the processing topology.
    // We opt for this unconditional call here because this will make it easier for you to play around with the example
    // when resetting the application for doing a re-run (via the Application Reset Tool,
    // https://docs.confluent.io/platform/current/streams/developer-guide/app-reset-tool.html).
    //
    // The drawback of cleaning up local state prior is that your app must rebuilt its local state from scratch, which
    // will take time and will require reading all the state-relevant data from the Kafka cluster over the network.
    // Thus in a production scenario you typically do not want to clean up always as we do here but rather only when it
    // is truly needed, i.e., only under certain conditions (e.g., the presence of a command line flag for your app).
    // See `ApplicationResetExample.java` for a production-like example.
    System.out.println("Cleanup start");
    streams.cleanUp();
    System.out.println("Cleanup end");

    final CountDownLatch latch = new CountDownLatch(1);
    // Attach shutdown handler to catch Control-C.
    Runtime.getRuntime().addShutdownHook(new Thread("streams-shutdown-hook") {
      @Override
      public void run() {
        streams.close(Duration.ofSeconds(5));
        latch.countDown();
      }
    });

    // Now run the processing topology via `start()` to begin processing its input data.
    streams.start();

  }

  /**
   * Configure the Streams application.
   * <p>
   * Various Kafka Streams related settings are defined here such as the location of the target Kafka cluster to use.
   * Additionally, you could also define Kafka Producer and Kafka Consumer settings when needed.
   *
   * @param bootstrapServers Kafka cluster address
   * @return Properties getStreamsConfiguration
   */
  static Properties getStreamsConfiguration(final String bootstrapServers) {
    final Properties streamsConfiguration = new Properties();
    // Give the Streams application a unique name.  The name must be unique in the Kafka cluster
    // against which the application is run.
    streamsConfiguration.put(StreamsConfig.APPLICATION_ID_CONFIG, "join-lambda-example");
    streamsConfiguration.put(StreamsConfig.CLIENT_ID_CONFIG, "join-lambda-example-client");
    // Where to find Kafka broker(s).
    streamsConfiguration.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    streamsConfiguration.put(StreamsConfig.REPLICATION_FACTOR_CONFIG, "2");



    streamsConfiguration.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class.getName());





    // Specify default (de)serializers for record keys and for record values.
//    streamsConfiguration.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
//    streamsConfiguration.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
    // Records should be flushed every 10 seconds. This is less than the default
    // in order to keep this example interactive.
    streamsConfiguration.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 10 * 1000);
    // For illustrative purposes we disable record caches.
    streamsConfiguration.put(StreamsConfig.CACHE_MAX_BYTES_BUFFERING_CONFIG, 0);

//    streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    streamsConfiguration.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
    // Use a temporary directory for storing state, which will be automatically removed after the test.
    streamsConfiguration.put(StreamsConfig.STATE_DIR_CONFIG, "c:\\temp\\kafka\\");


//    streamsConfiguration.put(AbstractKafkaAvroSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG, "https://schema-registry.non-prod.cloud.corp.stokrotka.pl");


    streamsConfiguration.put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, "SSL");
    streamsConfiguration.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, "server.truststore.jks");
    streamsConfiguration.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, "secret");
    streamsConfiguration.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, "client.jks");
    streamsConfiguration.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, "secret");
    streamsConfiguration.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, "secret");



    return streamsConfiguration;
  }

/*
  private SpecificAvroSerde<TicketSale> ticketSaleSerde(final Properties allProps) {
    final SpecificAvroSerde<TicketSale> serde = new SpecificAvroSerde<>();
    Map<String, String> config = (Map)allProps;
    serde.configure(config, false);
    return serde;
  }

*/

  /**
   * Define the processing topology for Word Count.
   *
   * @param builder StreamsBuilder to use
   */
  static void createWordCountStream(Properties allProps,final StreamsBuilder builder) {
    // Construct a `KStream` from the input topic "streams-plaintext-input", where message values
    // represent lines of text (for the sake of this example, we ignore whatever may be stored
    // in the message keys).  The default key and value serdes will be used.
//    final KStream<String, String> textLines = builder.stream(inputTopic);





    Map<String, Object> serdeProps = new HashMap<>();
    serdeProps.put("JsonPOJOClass", Movie2.class);


    final Serializer<Movie2> movieSerializer = new JsonPOJOSerializer<>();
    movieSerializer.configure(serdeProps, false);

    final Deserializer<Movie2> movieDeserializer = new JsonPOJODeserializer<>();
    movieDeserializer.configure(serdeProps, false);

    final Serde<Movie2> movieSerde = Serdes.serdeFrom(movieSerializer, movieDeserializer);

    MovieRatingJoiner joiner = new MovieRatingJoiner();

/*
    // When you want to override serdes explicitly/selectively
    final Map<String, String> serdeConfig = Collections.singletonMap("schema.registry.url",
            "https://schema-registry.non-prod.cloud.corp.stokrotka.pl");
    final Serde<Movie> valueSpecificAvroSerde = new SpecificAvroSerde<>();
    valueSpecificAvroSerde.configure(serdeConfig, false); // `false` for record values

    KStream<String, Movie> movieStream = builder.<String, Movie>stream(movieTopic);
*/

    final KStream<String, Movie2> movieStream =

            builder.stream(movieTopic,
                    Consumed.with(
                            Serdes.String(),
                            movieSerde)
            );


    System.out.println("XXXXXXXXXXXXXXXXXXXX");


    movieStream.map((key, movie) -> new KeyValue<>(String.valueOf(movie.getId()), movie))


            .peek((key, value) -> System.out.println("kStream : key="+key+", value="+value+" "+value.getClass().getName()  ));

    System.out.println("XXXXXXXXXXXXXXXXXXXX");


//    movieStream.to(rekeyedMovieTopic);
    movieStream.to(rekeyedMovieTopic, Produced.with(Serdes.String(), movieSerde));
//    movieStream.to(rekeyedMovieTopic, Produced.with(Serdes.String(), Serdes.String()));

    /*
    KTable<String, Movie> movies = builder.table(rekeyedMovieTopic);

    KStream<String, Rating> ratings = builder.<String, Rating>stream(ratingTopic)
            .map((key, rating) -> new KeyValue<>(String.valueOf(rating.getId()), rating));

    KStream<String, RatedMovie> ratedMovie = ratings.join(movies, joiner);

//    ratedMovie.to(ratedMoviesTopic, Produced.with(Serdes.String(), ratedMovieAvroSerde(allProps)));
//    ratedMovie.to(ratedMoviesTopic, Produced.with(Serdes.String(), Serdes.String()));
    ratedMovie.to(ratedMoviesTopic);

*/

  }

/*
  private static SpecificAvroSerde<RatedMovie> ratedMovieAvroSerde(Properties allProps) {
    SpecificAvroSerde<RatedMovie> movieAvroSerde = new SpecificAvroSerde<>();
    movieAvroSerde.configure((Map)allProps, false);
    return movieAvroSerde;
  }

  */
}