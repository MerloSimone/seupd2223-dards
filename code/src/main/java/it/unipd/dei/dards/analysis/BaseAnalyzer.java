/*
 * Copyright 2021-2022 University of Padua, Italy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package it.unipd.dei.dards.analysis;

import org.apache.lucene.analysis.*;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.boost.DelimitedBoostTokenFilterFactory;
import org.apache.lucene.analysis.classic.ClassicTokenizerFactory;
import org.apache.lucene.analysis.commongrams.CommonGramsFilterFactory;
import org.apache.lucene.analysis.core.*;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.*;
import org.apache.lucene.analysis.miscellaneous.FingerprintFilterFactory;
import org.apache.lucene.analysis.miscellaneous.HyphenatedWordsFilterFactory;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilterFactory;
import org.apache.lucene.analysis.ngram.NGramFilterFactory;
import org.apache.lucene.analysis.ngram.NGramTokenizerFactory;
import org.apache.lucene.analysis.opennlp.OpenNLPPOSFilterFactory;
import org.apache.lucene.analysis.opennlp.OpenNLPTokenizerFactory;
import org.apache.lucene.analysis.path.PathHierarchyTokenizerFactory;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.synonym.SynonymGraphFilterFactory;
import org.apache.lucene.analysis.wikipedia.WikipediaTokenizerFactory;

import java.io.IOException;
import java.io.Reader;

import static it.unipd.dei.dards.analysis.AnalyzerUtil.consumeTokenStream;
import static it.unipd.dei.dards.analysis.AnalyzerUtil.loadStopList;

/**
 * Introductory example on how to use write your own {@link BaseAnalyzer} by using different {@link Tokenizer}s and {@link
 * org.apache.lucene.analysis.TokenFilter}s.
 *
 * @author Simone Merlo (simone.merlo@studenti.unipd.it)
 * @version 1.0
 * @since 1.0
 */
public class BaseAnalyzer extends Analyzer {

    /**
     * Creates a new instance of the analyzer.
     */
    public BaseAnalyzer() {
        super();
    }

    @Override
    protected TokenStreamComponents createComponents(String fieldName) {

        //final Tokenizer source = new WhitespaceTokenizer();
        //final Tokenizer source = new LetterTokenizer();
        final Tokenizer source = new StandardTokenizer();

        TokenStream tokens = new LowerCaseFilter(source);

        //tokens = new LengthFilter(tokens, 4, 10);

        //tokens = new EnglishPossessiveFilter(tokens);

        tokens = new StopFilter(tokens, loadStopList("smart.txt"));

        //tokens = new PorterStemFilter(tokens);

        //tokens = new EnglishMinimalStemFilter(tokens);
        //tokens = new PorterStemFilter(tokens);
        tokens = new KStemFilter(tokens);
        // tokens = new LovinsStemFilter(tokens);

        //tokens = new NGramTokenFilter(tokens, 3);

        // tokens = new ShingleFilter(tokens, 3);

        return new TokenStreamComponents(source, tokens);
    }

    @Override
    protected Reader initReader(String fieldName, Reader reader) {
        // return new HTMLStripCharFilter(reader);

        return super.initReader(fieldName, reader);
    }

    @Override
    protected TokenStream normalize(String fieldName, TokenStream in) {
        return new LowerCaseFilter(in);
    }

    /**
     * Main method of the class.
     *
     * @param args command line arguments.
     *
     * @throws IOException if something goes wrong while processing the text.
     */
    public static void main(String[] args) throws IOException {

        // text to analyze
        //final String text = "I now live in Rome where I met my wife Alice back in 2010 during a beautiful afternoon
        // . " + "Occasionally, I fly to New York to visit the United Nations where I would like to work. The last " + "time I was there in March 2019, the flight was very inconvenient, leaving at 4:00 am, and expensive," + " over 1,500 dollars.";
        final String text = "Roadtrip USA: what insurance for car rental?\n" +
                "| Travel Blog Roadtrip USA: What is car rental insurance? Published by Chris on April 18, 201818 April 2018\n" +
                "I have nothing but contempt for journalists and bloggers who play on fear to advise you to do this or that.\n" +
                "However, there’s ONE thing I’ll never quibble about again: the importance of having insurance when you start a roadtrip in the US, especially with a car rental.\n" +
                "NE PARTEZ JAMAIS EN ROADTRIP\n" +
                "WITHOUT INSURANCE\n" +
                "VOYAGE\n" +
                "–\n" +
                "c’\n" +
                "is what we did, and we twice missed a very, very big disaster.\n" +
                "I didn’t mention it in those first tips for organising a roadtrip in the USA but, since I was recently asked the question, I am writing a follow-up article addressing this question.\n" +
                "Especially since too many people go on a roadtrip to the United States thinking only that their bank card will be enough to cover the bare minimum.\n" +
                "It’s FALSE, and all the more wrong if you don’t have a top-of-the-range Visa Premier card .\n" +
                "I will return to this point in the article.\n" +
                "Traffic accident on Louisiana highway\n" +
                "When we left for our roadtrip in the USA , we simply took out the insurance of the person who rented our vehicle, relying on the insurance of the VISA card with which we had paid for the car rental.\n" +
                "However, after passing very, very, very close to a traffic accident on a Louisiana highway, we found out a bit about how things would have gone if the accident had occurred (a serious collision resulting in potentially fatal injuries to the passengers of both vehicles).\n" +
                "We were wrong: I hadn’t seen a vehicle that was overtaking me and yet driving at a normal pace, and I gave it a head-scratcher that would have been much more dramatic if it had braked instead of speeding up when it saw me in line on the left.\n" +
                "After the trauma of having been close to the disaster, I took advantage of the ensuing insomnia to look for information on the management of hospitalization, lawyers’ fees, vehicle repairs, etc.\n" +
                "Needless VISA insurance First observation: if there had been injuries, the insurance on my VISA PREMIER card would have been of no use to me:\n" +
                "– the duration of the car rental must not exceed 31 days in total (and we have left for two months)\n" +
                "– the driver must be the insured (it was, but it would not have been if my companion had been at the wheel)\n" +
                "– above all, civil liability is limited to € 1 525 000 On the blue Ridge\n" +
                "Parkway In the United States\n" +
                ", liability insurance of €1,525,000 is not enough What? €1,525,000 of insurance for a car rental in the USA is not enough?\n" +
                "Well, no, not in the US, where the slightest misstep can cost you an arm if you are wrong.\n" +
                "There, legal proceedings are going on like a bunch, and causing a road accident can cost you very, very dearly.\n" +
                "We are talking about several million euros\n" +
                "– think that a single night in emergencies can cost you tens of thousands of dollars a day.\n" +
                "Imagine that you cause an accident resulting in a couple being hospitalized for several weeks: I’ll let you do the math.\n" +
                "Now imagine that they are taking you to court, with lawyers’ fees, accommodation, and possible damages to pay, and you will understand that €1,525,000 is not enough.\n" +
                "As a personal matter, I would therefore strongly recommend that you look for travel insurance that offers a very high guarantee of civil liability, such as Allianz Travel whose guarantee goes up to €4,500,000.\n" +
                "Consider that they didn’t come up with that number at random, and that in the US, it’s not an unnecessary whim to avoid taking the risk of ruining your life.\n" +
                "In a more mundane way, a simple last-minute medical consultation can cost you several hundred dollars – by then you will have made your insurance more than worthwhile (“Long Voyage” from Allianz, for stays of 2 months to 3 years, starts at €109 for two months, but instead costs €82 / month on average).\n" +
                "In our case, we learned with spite that the insurance of the person who rented our vehicle would not have covered us (it would only have covered the material damage), and that it was too late to take out travel insurance from the United States, since we had already left French soil.\n" +
                "Attention:\n" +
                "to be covered, you must be covered by a primary health insurance scheme.\n" +
                "However, after 183 days without a contribution, the cancellation of the CPAM is automatic.\n" +
                "If you are on a World Tour, ask about it.\n" +
                "EDIT 26/06/2018: as one reader quite rightly pointed out (thank you, Joseph!), it turns out that civil liability coverage does NOT cover the insured “if the damage was caused on board a motor vehicle.”\n" +
                "Likewise for Visa Premier cards, and, as Joseph says, “it is therefore imperative to have the Liability Insurance Supplement (LIS) in order to be properly insured in your rental vehicle in the United States.”\n" +
                "Thanks again to you for your vigilance, and forgive me for this big mistake!\n" +
                "A cariole as one crosses by dozens in the Amish Quid Country of material damage?\n" +
                "Since the rental period exceeded 31 days, the Visa Premier card would not have covered anything (and a traditional Visa card would not have covered anything at all,\n" +
                "even for a one-day rental)\n" +
                ".\n" +
                "Our vehicle owner’s insurance would have covered some of the repairs, but we would still be several thousand dollars out of pocket.\n" +
                "If we had opted for a classic car rental, passing through a large Avis, Hertz, Sixt , Carigami and cie type sign (as you no doubt will), the situation would have been very different and deserving\n" +
                "that a little time be spent on the issue in order to fully understand all the ins and outs.\n" +
                "A small lexicon of insurance options available for a Car Rental in the USA: – Liability insurance (LI) / Third Party Liability (TPL): Third Party Liability insurance that will not benefit you, as it is often limited to $5,000 (see above) – Supplemental Liability\n" +
                "Insurance (SLI) / Liability\n" +
                "Insurance\n" +
                "Additional Liability Insurance: an additional liability insurance, which is a must if you have no insurance and no high-end credit card (the standard Visa card does not cover RIEN).\n" +
                "$1 million is a MINIMUM, knowing it won't be enough in the event of a serious accident – Collision Damage Waiver (CDW) / Loss Damage Waiver (LDW): it's the most important insurance to have!\n" +
                "It will cover the cost of repairing the vehicle in the event of theft, accident or vandalism, except for breaking ice, puncturing tires and the interior of the vehicle.\n" +
                "Check that this insurance covers at least $1,000 in repairs.\n" +
                "– Theft Protection (TP): often included in the CDW / LDW, this insurance covers you if the vehicle is stolen –\n" +
                "Personal\n" +
                "Accident Insurance (PAI):\n" +
                "No need to take out this insurance\n" +
                "if you already have a high-end bank card\n" +
                "First Visa)\n" +
                "–\n" +
                "Franchise buy-back:\n" +
                "if you can afford it, I strongly advise you to take out an insurance policy that offers a deductible buy-out, as this will ensure that you do not have to pay out of pocket for any worries, so that you have the most peace of mind possible.\n" +
                "Often offered by high-end car leasers, it is also sometimes offered by travel insurance such as the one I quoted above TRUDO · 23 May 2018 at 5 h 42 min Generally, when renting a car insurance goes without saying, but you really have to spend a lot of time on details and conditions to be sure that you don’t get the wrong insurance and get insurance that allows you to drive around without too much worry and even more so for a roadtrip,\n" +
                "Thank you for the very helpful advice.\n" +
                "Joseph · June 25, 2018 at 2:53 pm Good morning, thank you for all the info about the need for insurance in the USA!\n" +
                "However, when reading the Allianz Travel insurance exclusions, I noticed that foreign private life liability cover of up to 4.5 million euros is not available if the damage was caused on board a motor vehicle: https://www.allianz-voyage.fr/infos-pratiques/tout-savoir-sur-l'assurance/que-ce-que-l'assurance-responsabilite-civile/ (cf Limitations and exclusions of warranty) Same for Visa Premier cards, Liability\n" +
                "It is therefore imperative to have the Liability Insurance Supplement (LIS) in order to be properly insured in your rental vehicle in the USA.\n" +
                "I hope this info has helped you!\n" +
                "Chris\n" +
                "26 June 2018 at 05:34 min Ohlala but you are 100% right …\n" +
                "I am appalled that I made such an error in my article.\n" +
                "I change it immediately!\n" +
                "A very, very big thank you to you for this comment and for your vigilance !!!\n" +
                "Hello, thanks for all these tips.\n" +
                "So if I plan to rent a camper van in the US for about 30 days, I have to pay the Liability Insurance Supplement (LIS) but it also does not insure my person or my spouse in case of an accident, I also have to take out travel insurance for possible medical expenses?\n" +
                "Basically you have to take all the insurance that comes up?\n" +
                "Thank you in advance!\n" +
                "Your e-mail address will not be published.\n" +
                "Mandatory fields are indicated\n" +
                "with * paintbrushes\n";

        // use the analyzer to process the text and print diagnostic information about each token
        //consumeTokenStream(new BaseAnalyzer(), text);

        final Analyzer a = CustomAnalyzer.builder().withTokenizer(StandardTokenizerFactory.class)
                .addTokenFilter(LowerCaseFilterFactory.class)
                .addTokenFilter(StopFilterFactory.class)
                .addTokenFilter(KStemFilterFactory.class)
                //.addTokenFilter(NGramFilterFactory.NAME, "minGramSize", "3", "maxGramSize", "10")
                .build();
        consumeTokenStream(a, text);


    }

}
