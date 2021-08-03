package mk.itzone.kafkastreams.kafkastreamssample;

import mk.itzone.kafkastreams.avro.Movie;
import mk.itzone.kafkastreams.avro.RatedMovie;
import mk.itzone.kafkastreams.avro.RatedMovie2;
import mk.itzone.kafkastreams.avro.Rating;
import org.apache.kafka.streams.kstream.ValueJoiner;

public class MovieRatingJoiner implements ValueJoiner<Rating, Movie, RatedMovie> {

    public RatedMovie apply(Rating rating, Movie movie) {

        System.out.println("MovieRatingJoiner "+rating+ " / "+ movie);

//        return new RatedMovie2(movie.getId(), movie.getTitle(), movie.getReleaseyear(), rating.getRating());

        return RatedMovie.newBuilder()
                .setId(movie.getId())
                .setTitle(movie.getTitle())
                .setReleaseyear(movie.getReleaseyear())
                .setRating(rating.getRating())
                .build();
    }
}