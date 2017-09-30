import edu.stanford.nlp.ling.*;
import edu.stanford.nlp.pipeline.*;
import edu.stanford.nlp.util.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.io.*;
import java.util.stream.Collectors;

class Core {

    private static void printQuantities(String str, List<String> list, PrintWriter writer, Integer... minrep){
        Set<String> hs = new HashSet<>();
        List<Pair<Integer, String>> Quantities = new ArrayList<>();
        writer.println(str);
        hs.addAll(list);
        for(String s : hs) Quantities.add(Pair.makePair(Collections.frequency(list,s),s));
        Quantities.sort(((o1, o2) -> o1.first.compareTo(o2.first)));

        for(Pair<Integer,String> pair : Quantities )
            if(minrep.length > 0) {
                if (pair.first >= minrep[0])
                    writer.println(pair.first + "\t" + pair.second);
            }else
                writer.println(pair.first + "\t" + pair.second);
        hs.clear();
        Quantities.clear();
    }
    private static void spreadToCategory(Pair<String,String> pair,  List<String> Org,List<String> Money ,List<String> People){
        switch (pair.first) {
            case "ORGANIZATION":
                Org.add(pair.second);
                break;
            case "PERSON":
                People.add(pair.second);
                break;
            case "MONEY":
                Money.add(pair.second);
                break;
            default:
                System.err.println("what?");
                break;
        }
    }
    private static void makeAnnotationsFromFiles(String dirfiles, String putAnnotations) throws IOException {

        List<Path> paths = Files.find(Paths.get(dirfiles),2,(path, attr) ->
                String.valueOf(path).endsWith(".txt")).collect(Collectors.toList());

        Properties props = new Properties();
        props.put("annotators", "tokenize, ssplit, pos, lemma,  ner, entitymentions");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        for(Path path : paths){

            Scanner fscan = new Scanner(path.toFile());
            fscan.skip("Category1 Category2 Category3\n\n");
            Annotation document = new Annotation(fscan.nextLine());

            int T = 1;

            pipeline.annotate(document);
            List<CoreMap> sentences = document.get(CoreAnnotations.SentencesAnnotation.class);

            try {
                String fileName  = path.getFileName().toString();
                PrintWriter writer = new PrintWriter(putAnnotations + fileName.substring(0,fileName.lastIndexOf(".")) + ".ann", "UTF-8");
                writer.print("");

                for (CoreMap sentence : sentences) {
                    for (CoreMap entityMention : sentence.get(CoreAnnotations.MentionsAnnotation.class)) {

                        String word = entityMention.get(CoreAnnotations.TextAnnotation.class);

                        String ne = entityMention.get(CoreAnnotations.NamedEntityTagAnnotation.class);

                        if(ne.equals("MONEY") && word.startsWith("#") && document.toString().contains("£" + word.substring(1)) && !document.toString().contains(word))
                            word  = "£" + word.substring(1);

                        String line;

                        if (ne.equals("ORGANIZATION") || ne.equals("PERSON") || ne.equals("MONEY")) {
                            line = "T" + T + "\t" + ne + " " + (entityMention.get(CoreAnnotations.CharacterOffsetBeginAnnotation.class) + 31) + " " + (entityMention.get(CoreAnnotations.CharacterOffsetEndAnnotation.class) + 31) + "\t" + word;
                            writer.println(line);
                            T++;
                        }
                    }
                }
                writer.close();
            }
            catch (IOException e){
                System.err.println("Writer  error: " + e.getCause().toString());
            }
        }
    }
    private static void CalculateStatistics(String dirCorrect , String dirAnn, String putStat) throws IOException {

        List<Path> paths = Files.find(Paths.get(dirCorrect),2,(path, attr) ->
                String.valueOf(path).endsWith(".ann")).collect(Collectors.toList());

        List<String> OTruePositive = new ArrayList<>();
        List<String> MTruePositive = new ArrayList<>();
        List<String> PTruePositive = new ArrayList<>();

        List<String> OFalsePositive = new ArrayList<>();
        List<String> MFalsePositive = new ArrayList<>();
        List<String> PFalsePositive = new ArrayList<>();

        List<String> OFalseNegative = new ArrayList<>();
        List<String> MFalseNegative = new ArrayList<>();
        List<String> PFalseNegative = new ArrayList<>();


        for(Path path : paths){
            List<Pair<String,String>> CorrectPairs = new ArrayList<>();
            List<Pair<String,String>> AlgoPairs = new ArrayList<>();

            Scanner correct = new Scanner(path.toAbsolutePath().toFile());

            Scanner algo = new Scanner(new File (dirAnn + path.getFileName()));

            while (correct.hasNextLine()) {

                String[] splitted = correct.nextLine().split("\t");
                String type = splitted[1].split(" ")[0];
                if (type.equals("ORGANIZATION") || type.equals("MONEY") || type.equals("PERSON"))
                    CorrectPairs.add(Pair.makePair(type, splitted[2]));

            }

            while (algo.hasNextLine()) {
                String[] splitted = algo.nextLine().split("\t");
                String type = splitted[1].split(" ")[0];

                if (type.equals("ORGANIZATION") || type.equals("MONEY") || type.equals("PERSON"))
                    AlgoPairs.add(Pair.makePair(type, splitted[2]));

            }

            for (Pair<String,String> algopair : AlgoPairs )
                if(CorrectPairs.contains(algopair)){
                    spreadToCategory(algopair,OTruePositive,MTruePositive,PTruePositive);
                    CorrectPairs.remove(algopair);
                }
                else {
                    spreadToCategory(algopair,OFalsePositive,MFalsePositive,PFalsePositive);
                    CorrectPairs.remove(algopair);
                }

            for (Pair<String,String> leftinCorrect : CorrectPairs )
                spreadToCategory(leftinCorrect,OFalseNegative,MFalseNegative,PFalseNegative);
        }

        int O_TP = OTruePositive.size(), O_FP = OFalsePositive.size(), O_FN = OFalseNegative.size();
        int P_TP = PTruePositive.size(), P_FP = PFalsePositive.size(), P_FN = PFalseNegative.size();
        int M_TP = MTruePositive.size() , M_FP = MFalsePositive.size() , M_FN = MFalseNegative.size() ;

        float O_percision = (float) O_TP/(O_FP+O_TP);
        float P_percision = (float) P_TP/(P_FP+P_TP);
        float M_percision = (float) M_TP/(M_FP+M_TP);

        float O_recall = (float) O_TP/(O_FN+O_TP);
        float P_recall = (float) P_TP/(P_FN+P_TP);
        float M_recall = (float) M_TP/(M_FN+M_TP);

        float O_Fmeasure = 2* O_percision*O_recall/(O_percision+O_recall);
        float P_Fmeasure = 2* P_percision*P_recall/(P_percision+P_recall);
        float M_Fmeasure = 2* M_percision*M_recall/(M_percision+M_recall);


        PrintWriter writer = new PrintWriter(putStat + "stistic.txt","UTF-8");

        writer.println("Organization\tTP\t" + O_TP  + "\tFP " + O_FP  + "\tFN " + O_FN + "\tF-measure " + O_Fmeasure);
        writer.println("Person\t\t\tTP\t" + P_TP + "\t\tFP " + P_FP + "\tFN " + P_FN + "\tF-measure " + P_Fmeasure);
        writer.println("Money\t\t\tTP\t" + M_TP + "\t\tFP " + M_FP + "\tFN " + M_FN + "\tF-measure " + M_Fmeasure);


        printQuantities("\nORGANIZATION\tTP\n", OTruePositive, writer,2);
        printQuantities("\nORGANIZATION\tFP\n", OFalsePositive, writer,2);
        printQuantities("\nORGANIZATION\tFN\n", OFalseNegative, writer,2);
        printQuantities("\nPERSON\tTP\n", PTruePositive, writer,2);
        printQuantities("\nPERSON\tFP\n", PFalsePositive, writer);
        printQuantities("\nPERSON\tFN\n", PFalseNegative, writer);
        printQuantities("\nMONEY\tTP\n", MTruePositive, writer);
        printQuantities("\nMONEY\tFP\n", MFalsePositive, writer);
        printQuantities("\nMONEY\tFN\n", MFalseNegative, writer);

        writer.close();
    }

    public static void main(String[] args) {
        String dir = "/home/vlad/Documents/CoreNLP/";
        /*
        try {
            makeAnnotationsFromFiles(dir + "DATA/", dir + "ANNOTATIONS/");
        }
        catch( IOException e ) { System.err.println("Error when searching files"); }
        */
        try {
            CalculateStatistics(dir + "DATA/" ,dir + "ANNOTATIONS/", dir);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}