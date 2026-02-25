package org.example.utils;

import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import lombok.experimental.UtilityClass;
import org.example.config.EsFieldsConfig;
import org.example.dto.QuestionAnswerDTO;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class AutoPartQueryUtil {

    public static Query createMainQueryWithSku(List<QuestionAnswerDTO> questionAnswerDTOS) {
        QuestionAnswerDTO skuQuestionAnswerDTO =
                questionAnswerDTOS
                        .stream()
                        .filter(questionAnswer -> questionAnswer.question().equals("sku"))
                        .findAny()
                        .orElseThrow(RuntimeException::new);

        return Query.of(q -> q.bool(builder -> builder
                .filter(filterBuilder -> filterBuilder
                        .term(t -> t.field(skuQuestionAnswerDTO.question())
                                .value(skuQuestionAnswerDTO.answer())
                        )))
        );
    }

    public static Query createMainQueryWithoutSku(List<QuestionAnswerDTO> questionAnswerDTOS) {

        List<Query> termsQuery = questionAnswerDTOS
                .stream()
                .map(questionAnswerDTO -> Query.of(b ->
                        b.term(t
                                -> t.field(questionAnswerDTO.question())
                                .value(questionAnswerDTO.answer()))))
                .toList();

        return Query.of(q -> q.bool(builder -> builder.filter(termsQuery)));
    }


    public static void addAggregationWithFilters(SearchRequest.Builder searchBuilder,
                                                 List<QuestionAnswerDTO> questionAnswerDTOS,
                                                 EsFieldsConfig config) {

        List<Query> mustQuestionAnswerQuery = new ArrayList<>();

        questionAnswerDTOS.forEach(questionAnswerDTO -> mustQuestionAnswerQuery.add(Query.of(q ->
                q.bool(builder -> builder.must(
                        mustBuilder -> mustBuilder.term(t -> t.field(questionAnswerDTO.question())
                                .value(questionAnswerDTO.answer())
                        ))))));


        searchBuilder.aggregations("strict_matches",
                b ->
                        b.filter(builder ->
                                        builder.bool(boolBuilder ->
                                                boolBuilder.must(mustQuestionAnswerQuery))
                                )
                                .aggregations("fit_docs",
                                        q -> q.topHits(topHitsBuilder -> topHitsBuilder.size(100))
                                ));
    }
}
