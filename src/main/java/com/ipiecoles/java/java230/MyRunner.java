package com.ipiecoles.java.java230;

import com.ipiecoles.java.java230.exceptions.BatchException;
import com.ipiecoles.java.java230.exceptions.TechnicienException;
import com.ipiecoles.java.java230.model.Commercial;
import com.ipiecoles.java.java230.model.Employe;
import com.ipiecoles.java.java230.model.Manager;
import com.ipiecoles.java.java230.model.Technicien;
import com.ipiecoles.java.java230.repository.EmployeRepository;
import com.ipiecoles.java.java230.repository.ManagerRepository;
import jdk.internal.org.objectweb.asm.tree.TryCatchBlockNode;
import org.aspectj.weaver.ast.Or;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.management.Query;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MyRunner implements CommandLineRunner {

    private static final String REGEX_MATRICULE = "^[MTC][0-9]{5}$";
    private static final String REGEX_NOM = ".*";
    private static final String REGEX_PRENOM = ".*";
    private static final int NB_CHAMPS_MANAGER = 5;
    private static final int NB_CHAMPS_TECHNICIEN = 7;
    private static final String REGEX_MATRICULE_MANAGER = "^M[0-9]{5}$";
    private static final int NB_CHAMPS_COMMERCIAL = 7;

    @Autowired
    private EmployeRepository employeRepository;

    @Autowired
    private ManagerRepository managerRepository;

    private List<Employe> employes = new ArrayList<>();

    /** LOGGER : Permet d'afficher les informations et de les intégrer dans un fichier ou dans une BDD
     * il a plusieurs niveau : info, warm, error */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void run(String... strings) throws Exception {
        String fileName = "employes.csv";
        readFile(fileName);
        //readFile(strings[0]);
    }

    /**
     * Méthode qui lit le fichier CSV en paramètre afin d'intégrer son contenu en BDD
     * @param fileName Le nom du fichier (à mettre dans src/main/resources)
     * @return une liste contenant les employés à insérer en BDD ou null si le fichier n'a pas pu être le
     */
    public List<Employe> readFile(String fileName){
        Stream<String> stream;
        logger.info("Lecture du fichier : " + fileName);

        try {
            stream = Files.lines(Paths.get(new ClassPathResource(fileName).getURI()));
            }
        catch (IOException e)
            {
            logger.error("Problème dans l'ouverture du fichier " + fileName);
            //on renvoie une liste vide puisqu'on a pas pu ouvrir le fichier
            return new ArrayList<>();
            }

        List<String> lignes = stream.collect(Collectors.toList());
        logger.info(lignes.size()+ " lignes lues");
        for(int i = 0; 1 < lignes.size(); i++)
            {
                try {
                    processLine(lignes.get(i));
                } catch (Exception e) {
                    logger.error("Ligne " + (i+1) + " : " + e.getMessage() + " => " + lignes.get(i));
                }
            }
        return employes;
    }

    /**
     * Méthode qui regarde le premier caractère de la ligne et appelle la bonne méthode de création d'employé
     * @param ligne la ligne à analyser
     * @throws BatchException si le type d'employé n'a pas été reconnu
     */
    private void processLine(String ligne) throws BatchException {
        //TODO :
        switch (ligne.substring(0,1)){
            case "T" :
                processTechnicien(ligne);
                break;
            case "M" :
                processManager(ligne);
                break;
            case "C" :
                processCommercial(ligne);
                break;
            default:
                throw new BatchException("Type d'employé inconnu");
        }
    }

    /**
     * Méthode qui crée un Commercial à partir d'une ligne contenant les informations d'un commercial et l'ajoute dans la liste globale des employés
     * @param ligneCommercial la ligne contenant les infos du commercial à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processCommercial(String ligneCommercial) throws BatchException {

        String[] commercialFields = ligneCommercial.split(",");

        // nombre de champs
        if (commercialFields.length != NB_CHAMPS_COMMERCIAL)
            {
            throw new BatchException("La ligne commercial ne contient pas " + NB_CHAMPS_COMMERCIAL + " éléments mais " + commercialFields.length );
            }

        // contrôle le matricule
        if (!commercialFields[0].matches(REGEX_MATRICULE))
            {
            throw new BatchException("la chaîne " + commercialFields[0] + " ne respecte pas l'expression régulière " + REGEX_MATRICULE);
            }

        // contrôle de la date
        LocalDate D = null ;
        try {
            D = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(commercialFields[3]);
        } catch (Exception e) {
            throw new BatchException(commercialFields[3] + " ne respecte pas le format de date dd/MM/yyyy" );
        }

        // contrôle du salaire
        double S ;
        try {
            S = Double.parseDouble(commercialFields[4]);
        } catch (Exception e) {
            throw new BatchException(commercialFields[4] + " n'est pas un nombre valide pour un salaire" );
        }

        // contrôle du chiffre d'affaire
        double CA ;
        try {
            CA = Double.parseDouble(commercialFields[5]);
        } catch (Exception e) {
            throw new BatchException("Le chiffre d'affaire du commercial est incorrect : " + commercialFields[5]);
        }

        // contrôle de la performance
        int P ;
        try {
            P = Integer.parseInt(commercialFields[6]);
        } catch (Exception e) {
            throw new BatchException("La performance du commercial est incorrecte : " + commercialFields[6]);
        }

        // ajout en base de données
        Commercial c = new Commercial();
        c.setMatricule(commercialFields[0]);
        c.setNom(commercialFields[1]);
        c.setPrenom(commercialFields[2]);
        c.setDateEmbauche(D);
        c.setSalaire(S);
        c.setCaAnnuel(CA);
        c.setPerformance(P);

        employes.add(c);

    }

    /**
     * Méthode qui crée un Manager à partir d'une ligne contenant les informations d'un manager et l'ajoute dans la liste globale des employés
     * @param ligneManager la ligne contenant les infos du manager à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processManager(String ligneManager) throws BatchException {

        String[] managerFields = ligneManager.split(",");

        // nombre de champs
        if (managerFields.length != NB_CHAMPS_MANAGER)
        {
            throw new BatchException("La ligne manager ne contient pas " + NB_CHAMPS_MANAGER + " éléments mais " + managerFields.length );
        }

        // contrôle le matricule
        if (!managerFields[0].matches(REGEX_MATRICULE_MANAGER))
        {
            throw new BatchException("la chaîne ne respecte pas l'expression régulière " + REGEX_MATRICULE_MANAGER);
        }

        // contrôle de la date
        LocalDate D = null;
        try {
            D = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(managerFields[3]);
        } catch (Exception e) {
            throw new BatchException(managerFields[3] + " ne respecte pas le format de date dd/MM/yyyy" );
        }

        // contrôle du salaire
        double Salaire ;
        try {
            Salaire = Double.parseDouble(managerFields[4]);
        } catch (Exception e) {
            throw new BatchException(managerFields[4] + " n'est pas un nombre valide pour un salaire" );
        }


        // ajout en base de données
        Manager m = new Manager();
        m.setMatricule(managerFields[0]);
        m.setNom(managerFields[1]);
        m.setPrenom(managerFields[2]);
        m.setDateEmbauche(D);
        m.setSalaire(Salaire);

        employes.add(m);
    }

    /**
     * Méthode qui crée un Technicien à partir d'une ligne contenant les informations d'un technicien et l'ajoute dans la liste globale des employés
     * @param ligneTechnicien la ligne contenant les infos du technicien à intégrer
     * @throws BatchException s'il y a un problème sur cette ligne
     */
    private void processTechnicien(String ligneTechnicien) throws BatchException {

        String[] technicienFields = ligneTechnicien.split(",");

        // nombre de champs
        if (technicienFields.length != NB_CHAMPS_TECHNICIEN)
        {
            throw new BatchException("La ligne commercial ne contient pas " + NB_CHAMPS_TECHNICIEN + " éléments mais " + technicienFields.length );
        }

        // contrôle le matricule
        if (!technicienFields[0].matches(REGEX_MATRICULE))
        {
            throw new BatchException("la chaîne ne respecte pas l'expression régulière " + REGEX_MATRICULE);
        }

        // contrôle de la date
        LocalDate D = null ;
        try {
            D = DateTimeFormat.forPattern("dd/MM/yyyy").parseLocalDate(technicienFields[3]);
        } catch (Exception e) {
            throw new BatchException(technicienFields[3] + " ne respecte pas le format de date dd/MM/yyyy" );
        }

        // contrôle du salaire
        double Salaire ;
        try {
            Salaire = Double.parseDouble(technicienFields[4]);
        } catch (Exception e) {
            throw new BatchException(technicienFields[4] + " n'est pas un nombre valide pour un salaire" );
        }

        // contrôle du grade
        int grade ;
        try {
            grade = Integer.parseInt(technicienFields[5]);
        } catch (Exception e) {
            throw new BatchException("Le grade du technicien est incorrect : " + technicienFields[5] );
        }
//        if (grade < 1 || grade > 5){
//            throw new BatchException("Le grade doit être compris entre 1 et 5 :" );
//        }

        // contrôle du matricule du manager
        if (!technicienFields[6].matches(REGEX_MATRICULE_MANAGER))
        {
            throw new BatchException("La chaîne " + technicienFields[6] + " ne respecte pas l'expression régulière " + REGEX_MATRICULE_MANAGER);
        }

        //vérification existance
        if (managerRepository.findByMatricule(technicienFields[6]) == null)
        {
            throw new BatchException("Le manager de matricule " + technicienFields[6] + " n'a pas été trouvé dans le fichier ou en base de données ");
        }

        // ajout en base de données
        Technicien t = new Technicien();
        t.setMatricule(technicienFields[0]);
        t.setNom(technicienFields[1]);
        t.setPrenom(technicienFields[2]);
        t.setDateEmbauche(D);
        try {
            t.setGrade(grade);
            }
        catch (TechnicienException e){
            throw new BatchException("Le grade doit être compris entre 1 et 5 :" );
        }

        t.setSalaire(Salaire);
        t.setManager(managerRepository.findByMatricule(technicienFields[6]));
        employes.add(t);




    }






}
