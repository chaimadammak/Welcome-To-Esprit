package tn.esprit.springfever.Services.Implementation;

import com.itextpdf.text.Annotation;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.StringUtils;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.util.CoreMap;
import lombok.extern.slf4j.Slf4j;
import opennlp.tools.chunker.Chunker;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.*;
import opennlp.tools.util.Span;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tn.esprit.springfever.Services.Interfaces.IJobApplication;
import tn.esprit.springfever.entities.Image_JobOffer;
import tn.esprit.springfever.entities.Job_Application;
import tn.esprit.springfever.entities.Job_Offer;
import tn.esprit.springfever.entities.User;
import tn.esprit.springfever.repositories.JobApplicationRepository;
import tn.esprit.springfever.repositories.JobApplicatonPdfRepository;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;


import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;

import java.io.IOException;
import java.util.Properties;





@Service
@Slf4j
public class JobApplicationService implements IJobApplication {

    @Autowired
    JobApplicationRepository jobApplicationRepository;
    @Autowired
    JobApplicatonPdfRepository jobApplicatonPdfRepository;

    @Autowired
    private JavaMailSender mailSender;


    public Job_Application AddJobApplication (Job_Application job_application){
       return jobApplicationRepository.save(job_application);

    }
    public Job_Application save(byte[] cv, byte[] lettre, String imageName) throws Exception {
        //String location = jobApplicatonPdfRepository.save(cv,lettre,imageName);
        return jobApplicationRepository.save(new Job_Application(cv, lettre));
    }


    public List<Job_Application> GetAllJobApplications(){
        return jobApplicationRepository.findAll();

    }

    public String DeleteJobApplication (Long Id_Job_Application){
        Job_Application JobApplicationExisted=jobApplicationRepository.findById(Id_Job_Application).orElse(null);
        if(JobApplicationExisted!=null){
            jobApplicationRepository.delete(JobApplicationExisted);
            return "Job Application is Deleted !";
        }
        return "Job Application Does not Exist";

    }

    /*public Job_Application UpdateJobApplication(Long Id_Job_Application , Job_Application job_application){
        Job_Application JobApplicationExisted=jobApplicationRepository.findById(Id_Job_Application).orElse(null);
        if(JobApplicationExisted!=null){
            JobApplicationExisted.setJobOffer(job_application.getJobOffer());
            JobApplicationExisted.setCv(job_application.getCv());
            JobApplicationExisted.setUser(job_application.getUser());
            JobApplicationExisted.setRdv(job_application.getRdv());
            JobApplicationExisted.setLettreMotivation(job_application.getLettreMotivation());
            return jobApplicationRepository.save(JobApplicationExisted);
        }
        log.info("Job Application does not exist ! ");
        return JobApplicationExisted;

    }*/

    public Job_Application savef(byte[] cv, byte[] lettre, String location_Cv, String location_LettreMotivation) throws Exception {
        Path cvFile = Paths.get("C:\\Users\\User\\Desktop\\" + new Date().getTime() + "-" + location_Cv);
        Path lettreFile = Paths.get("C:\\Users\\User\\Desktop\\" + new Date().getTime() + "-" + location_LettreMotivation);

        Files.write(cvFile, cv);
        Files.write(lettreFile, lettre);

        String cvLocation = cvFile.toAbsolutePath().toString();
        String lettreLocation = lettreFile.toAbsolutePath().toString();

        return jobApplicationRepository.save(new Job_Application( cvLocation, lettreLocation));
    }

    public FileSystemResource findCV(Long Id_Job_Application) {

        Job_Application job_application = jobApplicationRepository.findById(Id_Job_Application)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return jobApplicatonPdfRepository.findInFileSystem(job_application.getLocation_Cv());
    }

    public FileSystemResource findLettreMotivation(Long Id_Job_Application) {

        Job_Application job_application = jobApplicationRepository.findById(Id_Job_Application)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return jobApplicatonPdfRepository.findInFileSystem(job_application.getLocation_LettreMotivation());
    }

    public Boolean FilterCv(Long Id_Job_Application){

        Job_Application job_application=jobApplicationRepository.findById(Id_Job_Application).orElse(null);
        Job_Offer job_offer=job_application.getJobOffer();
        String text= extractTextFromPdf(Id_Job_Application);
        String text2=job_offer.getSubject();
        System.out.println(text);
        System.out.println(text2);
        if(text.contains(text2)) {
            System.out.println("CV is Accepted  \n" +
                    "It contains the skills sought by the job offer\n");
            return true ;


        } else {
            System.out.println("CV is Not Accepted  \n" +
                    "It Not contains the skills sought by the job offer\n");
            return false ;

        }

    }


    public String extractTextFromPdf(Long id){
        String text = null;
        try {
            FileSystemResource fileSystemResource = findLettreMotivation(id);

            PdfReader reader = new PdfReader(fileSystemResource.getPath());

            int n = reader.getNumberOfPages();
            text = "";
            for (int i = 0; i < n; i++) {
                text += PdfTextExtractor.getTextFromPage(reader, i + 1).trim() + "\n";
            }
            reader.close();
            System.out.println(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return text;
    }


    public void sendEmail(Long id, String subject, String body){
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("chaima.dammak@espri.tn");
        Job_Application job_application=jobApplicationRepository.findById(id).orElse(null);
        String to=job_application.getUser().getEmail();
        System.out.println(to);
        message.setTo(to);
        message.setSubject(subject);
        //body="Hello !! ";
        message.setText(body);
        mailSender.send(message);
    }

    public  String extractTextFromPdf2(Long id) throws IOException {
        Job_Application job_application=jobApplicationRepository.findById(id).orElse(null);
        File file = new File(job_application.getLocation_LettreMotivation());
        PDDocument document = PDDocument.load(file);
        PDFTextStripper stripper = new PDFTextStripper();
        String text = stripper.getText(document);
        document.close();
        return text;
    }



    //Bonne Code

    public String extractSkills(Long id) throws IOException {
        String text=extractTextFromPdf2(id);
        ClassLoader classLoader = getClass().getClassLoader();

        String sentenceModelPath = "C:/opennlp-en-ud-ewt-sentence-1.0-1.9.3.bin";

        String tokenizerModelPath = "C:/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin";
        String posModelPath = "C:/opennlp-en-ud-ewt-pos-1.0-1.9.3.bin";
        String chunkerModelPath = "/en-chunker.bin";


        // Load the model for sentence detection
        SentenceModel sentenceModel = new SentenceModel(new FileInputStream(sentenceModelPath));
        SentenceDetectorME sentenceDetector = new SentenceDetectorME(sentenceModel);

// Load the model for tokenization
        TokenizerModel tokenizerModel = new TokenizerModel(new FileInputStream(tokenizerModelPath));
        TokenizerME tokenizer = new TokenizerME(tokenizerModel);

// Load the model for POS tagging
        POSModel posModel = new POSModel(new FileInputStream(posModelPath));
        POSTaggerME posTagger = new POSTaggerME(posModel);

// Load the model for chunking
        ChunkerModel chunkerModel = new ChunkerModel(new FileInputStream(chunkerModelPath));
        ChunkerME chunker = new ChunkerME(chunkerModel);
        // Split the text into sentences
        String[] sentences = sentenceDetector.sentDetect(text);

        // Extract the skills from each sentence
        for (String sentence : sentences) {

            // Tokenize the sentence
            String[] tokens = tokenizer.tokenize(sentence);

            // Tag the parts of speech of each token
            String[] tags = posTagger.tag(tokens);

            // Chunk the tagged tokens to extract noun phrases
            Span[] chunks = chunker.chunkAsSpans(tokens, tags);

            // Extract the noun phrases that represent skills
            for (Span chunk : chunks) {
                String chunkText = "";
                for (int i = chunk.getStart(); i < chunk.getEnd(); i++) {
                    chunkText += tokens[i] + " ";
                }
                chunkText = chunkText.trim();
                if (chunk.getType().equals("NP") && isSkill(chunkText)) {
                    System.out.println(chunkText);
                    return chunkText;
                }

            }
        }
        if (text == null || text.trim().isEmpty()) {
            return "Aucun texte n'a été extrait du PDF";
        }

        return "chaima";


    }


//Ce code est le meme sauf qu'il retourne toute une liste de skills
    /*public String extractSkills(Long id) throws IOException {
        String text=extractTextFromPdf2(id);
        ClassLoader classLoader = getClass().getClassLoader();

        String sentenceModelPath = "C:/opennlp-en-ud-ewt-sentence-1.0-1.9.3.bin";

        String tokenizerModelPath = "C:/opennlp-en-ud-ewt-tokens-1.0-1.9.3.bin";
        String posModelPath = "C:/opennlp-en-ud-ewt-pos-1.0-1.9.3.bin";
        String chunkerModelPath = "/en-chunker.bin";


        // Load the model for sentence detection
        SentenceModel sentenceModel = new SentenceModel(new FileInputStream(sentenceModelPath));
        SentenceDetectorME sentenceDetector = new SentenceDetectorME(sentenceModel);

        // Load the model for tokenization
        TokenizerModel tokenizerModel = new TokenizerModel(new FileInputStream(tokenizerModelPath));
        TokenizerME tokenizer = new TokenizerME(tokenizerModel);

        // Load the model for POS tagging
        POSModel posModel = new POSModel(new FileInputStream(posModelPath));
        POSTaggerME posTagger = new POSTaggerME(posModel);

        // Load the model for chunking
        ChunkerModel chunkerModel = new ChunkerModel(new FileInputStream(chunkerModelPath));
        ChunkerME chunker = new ChunkerME(chunkerModel);

        // Split the text into sentences
        String[] sentences = sentenceDetector.sentDetect(text);

        // Extract the skills from each sentence
        String extractedSkills = "";
        for (String sentence : sentences) {

            // Tokenize the sentence
            String[] tokens = tokenizer.tokenize(sentence);

            // Tag the parts of speech of each token
            String[] tags = posTagger.tag(tokens);

            // Chunk the tagged tokens to extract noun phrases
            Span[] chunks = chunker.chunkAsSpans(tokens, tags);

            // Extract the noun phrases that represent skills
            for (Span chunk : chunks) {
                String chunkText = "";
                for (int i = chunk.getStart(); i < chunk.getEnd(); i++) {
                    chunkText += tokens[i] + " ";
                }
                chunkText = chunkText.trim();
                if (chunk.getType().equals("NP") && isSkill(chunkText)) {
                    extractedSkills += chunkText + ", ";
                }
            }
        }

        if (extractedSkills.isEmpty()) {
            return "Aucune compétence n'a été extraite du CV";
        } else {
            extractedSkills = extractedSkills.substring(0, extractedSkills.length() - 2); // Remove the last comma
            return extractedSkills;
        }
    }*/







    private  List<String> skillsList = Arrays.asList("Java", "Python", "JavaScript", "React", "Node.js","Html","test2");

    public   boolean isSkill(String word) {
        // Split the text into words
        String[] words = word.split(" ");
        // Extract the skills
        for (String a : words) {
            if (skillsList.contains(a)) {
                //System.out.println(a);
                return true;
            }
        }


        return false;
    }






}





