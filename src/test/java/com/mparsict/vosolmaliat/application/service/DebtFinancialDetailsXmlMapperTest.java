package com.mparsict.vosolmaliat.application.service;

import com.mparsict.vosolmaliat.domain.enums.PaperType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DebtFinancialDetailsXmlMapperTest {
    private final DebtPaperXmlReader reader = new DebtPaperXmlReader();

    @Test
    void ef03StoresEachCategoryAsXmlAndRetainsRepeatedPenaltyRowsInOrder() {
        var details = DebtFinancialDetailsXmlMapper.map(PaperType.EF03, reader.parse("""
                <content><MAIN><CB>100</CB><DB>7</DB><EB>10</EB><FB>90</FB>
                <GB>5</GB><HB>95</HB><KB>جریمه اول</KB><MB>2</MB>
                <KB>جریمه دوم</KB><MB>3</MB><NB>5</NB></MAIN></content>
                """));

        assertThat(details.getTaxLiabilityDetails()).contains("<CB>100</CB>");
        assertThat(details.getTotalRemissionsDetails()).contains("<DB>7</DB>");
        assertThat(details.getTotalPaymentsDetails()).contains("<EB>10</EB>");
        assertThat(details.getOutstandingDebtDetails()).containsSubsequence("<FB>90</FB>", "<HB>95</HB>");
        assertThat(details.getTotalPenaltiesDetails()).containsSubsequence(
                "<GB>5</GB>", "<KB>جریمه اول</KB>", "<MB>2</MB>",
                "<KB>جریمه دوم</KB>", "<MB>3</MB>", "<NB>5</NB>");
        assertThat(details.getTaxCreditDetails()).isEqualTo("<details/>");
    }

    @Test
    void ef23UsesOccurrenceOrderForTagsThatHaveTwoDictionaryMeanings() {
        var details = DebtFinancialDetailsXmlMapper.map(PaperType.EF23, reader.parse("""
                <content><MAIN><RD>11</RD><SD>12</SD><RD>21</RD><SD>22</SD></MAIN></content>
                """));

        assertThat(details.getTaxCreditDetails()).contains("<RD>11</RD>", "<SD>12</SD>")
                .doesNotContain("<RD>21</RD>", "<SD>22</SD>");
        assertThat(details.getTotalPaymentsDetails()).contains("<RD>21</RD>")
                .doesNotContain("<RD>11</RD>");
        assertThat(details.getOutstandingDebtDetails()).contains("<SD>22</SD>")
                .doesNotContain("<SD>12</SD>");
    }

    @Test
    void ef23TypeTwoSeparatesPrincipalPenaltyPaymentAndFinalBalance() {
        var details = DebtFinancialDetailsXmlMapper.map(PaperType.EF23_2, reader.parse("""
                <content><MAIN><QE>100</QE><RE>20</RE><ME>7</ME><OE>10</OE>
                <GE>120</GE><PE>117</PE></MAIN></content>
                """));

        assertThat(details.getTaxLiabilityDetails()).contains("<QE>100</QE>", "<RE>20</RE>");
        assertThat(details.getTotalPenaltiesDetails()).contains("<ME>7</ME>");
        assertThat(details.getTotalPaymentsDetails()).contains("<OE>10</OE>");
        assertThat(details.getOutstandingDebtDetails()).contains("<GE>120</GE>", "<PE>117</PE>");
    }
}
