package mk.itzone.kafkastreams.kafkastreamssample;
import com.fasterxml.jackson.annotation.JsonIgnore;
import org.apache.avro.generic.GenericArray;
import org.apache.avro.specific.SpecificData;
import org.apache.avro.util.Utf8;
import org.apache.avro.message.BinaryMessageEncoder;
import org.apache.avro.message.BinaryMessageDecoder;
import org.apache.avro.message.SchemaStore;


public class Movie2  {
  private static final long serialVersionUID = -2799904060346594507L;




   private long id;
   private java.lang.String title;
   private int releaseyear;

  /**
   * Default constructor.  Note that this does not initialize fields
   * to their default values from the schema.  If that is desired then
   * one should use <code>newBuilder()</code>.
   */
  public Movie2() {}

  /**
   * All-args constructor.
   * @param id The new value for id
   * @param title The new value for title
   * @param releaseyear The new value for releaseyear
   */
  public Movie2(java.lang.Long id, java.lang.String title, java.lang.Integer releaseyear) {
    this.id = id;
    this.title = title;
    this.releaseyear = releaseyear;
  }




  /**
   * Gets the value of the 'id' field.
   * @return The value of the 'id' field.
   */
  public long getId() {
    return id;
  }



  /**
   * Gets the value of the 'title' field.
   * @return The value of the 'title' field.
   */
  public java.lang.String getTitle() {
    return title;
  }



  /**
   * Gets the value of the 'releaseyear' field.
   * @return The value of the 'releaseyear' field.
   */
  public int getReleaseyear() {
    return releaseyear;
  }







}










