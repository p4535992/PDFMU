package cz.hobrasoft.pdfmu.signature;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.security.BouncyCastleDigest;
import com.itextpdf.text.pdf.security.CrlClient;
import com.itextpdf.text.pdf.security.DigestAlgorithms;
import com.itextpdf.text.pdf.security.ExternalDigest;
import com.itextpdf.text.pdf.security.ExternalSignature;
import com.itextpdf.text.pdf.security.MakeSignature;
import com.itextpdf.text.pdf.security.OcspClient;
import com.itextpdf.text.pdf.security.PrivateKeySignature;
import com.itextpdf.text.pdf.security.TSAClient;
import cz.hobrasoft.pdfmu.Operation;
import cz.hobrasoft.pdfmu.OperationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Collection;
import java.util.logging.Logger;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * Adds a digital signature to a PDF document
 *
 * @author <a href="mailto:filip.bartek@hobrasoft.cz">Filip Bartek</a>
 */
public class OperationSignatureAdd implements Operation {

    private static final Logger logger = Logger.getLogger(OperationSignatureAdd.class.getName());

    @Override
    public String getCommandName() {
        return "add";
    }

    @Override
    public Subparser configureSubparser(Subparser subparser) {
        String help = "Digitally sign a PDF document";

        String metavarIn = "IN.pdf";
        String metavarOut = "OUT.pdf";

        // Configure the subparser
        subparser.help(help)
                .description(help)
                .defaultHelp(true)
                .setDefault("command", OperationSignatureAdd.class);

        // Add arguments to the subparser
        // Positional arguments are required by default
        subparser.addArgument("in")
                .help("input PDF document")
                .metavar(metavarIn)
                .type(Arguments.fileType().acceptSystemIn().verifyCanRead())
                .required(true);

        subparser.addArgument("-o", "--out")
                .help(String.format("output PDF document (default: <%s>)", metavarIn))
                .metavar(metavarOut)
                .type(Arguments.fileType().verifyCanCreate())
                .nargs("?");
        subparser.addArgument("-f", "--force")
                .help(String.format("overwrite %s if it exists", metavarOut))
                .type(boolean.class)
                .action(Arguments.storeTrue());

        signatureParameters.addArguments(subparser);

        return subparser;
    }

    // digitalsignatures20130304.pdf : Code sample 1.6
    // Initialize the security provider
    private static final BouncyCastleProvider provider = new BouncyCastleProvider();

    static {
        // We need to register the provider because it needs to be accessible by its name globally.
        // {@link com.itextpdf.text.pdf.security.PrivateKeySignature#PrivateKeySignature(PrivateKey pk, String hashAlgorithm, String provider)}
        // uses the provider name.
        Security.addProvider(provider);
    }

    // Initialize the digest algorithm
    private static final ExternalDigest externalDigest = new BouncyCastleDigest();

    // `signatureParameters` is a member variable
    // so that we can add the arguments to the parser in `configureSubparser`.
    // We need an instance of {@link SignatureParameters} in `configureSubparser`
    // because the interface `ArgsConfiguration` does not allow static methods.
    private final SignatureParameters signatureParameters = new SignatureParameters();

    @Override
    public void execute(Namespace namespace) throws OperationException {
        // Input file
        File inFile = namespace.get("in");
        assert inFile != null; // Required argument

        // Output file
        File outFile = namespace.get("out");

        boolean append = true;
        // With `append == false`, adding a signature invalidates the previous signature.
        // In order to make `append == false` work correctly, we would need to remove the previous signature.

        // Initialize signature parameters
        signatureParameters.setFromNamespace(namespace);

        boolean forceOverwrite = namespace.getBoolean("force");
        // Note: "force" argument is required

        sign(inFile, outFile, forceOverwrite, append, signatureParameters);
    }

    // Open the PDF reader
    private static void sign(File inFile,
            File outFile,
            boolean forceOverwrite,
            boolean append,
            SignatureParameters signatureParameters) throws OperationException {
        assert inFile != null;

        // TODO: Remove one of the duplicit messages
        logger.info(String.format("Input PDF document: %s", inFile));

        // Open the input stream
        FileInputStream inStream;
        try {
            inStream = new FileInputStream(inFile);
        } catch (FileNotFoundException ex) {
            throw new OperationException("Input file not found.", ex);
        }

        // Open the PDF reader
        // PdfReader parses a PDF document.
        PdfReader pdfReader;
        try {
            pdfReader = new PdfReader(inStream);
        } catch (IOException ex) {
            throw new OperationException("Could not open the input PDF document.", ex);
        }

        // Set `outFile` to `inFile` if not set
        // Handle calls of type `pdfmu signature add INOUT.pdf`
        if (outFile == null) {
            logger.info("Output file not specified. Assuming in-place operation.");
            outFile = inFile;
        }

        sign(pdfReader, outFile, forceOverwrite, append, signatureParameters);

        // Close the PDF reader
        pdfReader.close();

        // Close the input stream
        try {
            inStream.close();
        } catch (IOException ex) {
            throw new OperationException("Could not close the input file.", ex);
        }
    }

    // Handle overwriting
    private static void sign(PdfReader pdfReader,
            File outFile,
            boolean forceOverwrite,
            boolean append,
            SignatureParameters signatureParameters) throws OperationException {
        assert outFile != null;

        logger.info(String.format("Output PDF document: %s", outFile));

        if (outFile.exists()) {
            logger.info("Output file already exists.");
            if (forceOverwrite) {
                logger.info("Overwriting the output file (--force flag is set).");
            } else {
                throw new OperationException("Set --force flag to overwrite.");
            }
        }

        sign(pdfReader, outFile, append, signatureParameters);
    }

    // Open the PDF stamper
    // Overwrite `outFile` if it already exists.
    private static void sign(PdfReader pdfReader,
            File outFile,
            boolean append,
            SignatureParameters signatureParameters) throws OperationException {
        assert outFile != null;

        // Open the output stream
        FileOutputStream os;
        try {
            os = new FileOutputStream(outFile);
        } catch (FileNotFoundException ex) {
            throw new OperationException("Could not open the output file.", ex);
        }

        // TODO: Remove the "append" option
        if (append) {
            logger.info("Appending signature.");
        } else {
            logger.info("Replacing signature.");
        }

        PdfStamper stp;
        try {
            // digitalsignatures20130304.pdf : Code sample 2.17
            // TODO?: Make sure version is high enough
            stp = PdfStamper.createSignature(pdfReader, os, '\0', null, append);
        } catch (DocumentException | IOException ex) {
            throw new OperationException("Could not open the PDF stamper.", ex);
        }

        sign(stp, signatureParameters);

        // Close the PDF stamper
        try {
            stp.close();
        } catch (DocumentException | IOException ex) {
            throw new OperationException("Could not close PDF stamper.", ex);
        }

        // Close the output stream
        try {
            os.close();
        } catch (IOException ex) {
            throw new OperationException("Could not close the output file.", ex);
        }
    }

    // Initialize the signature appearance
    private static void sign(PdfStamper stp,
            SignatureParameters signatureParameters) throws OperationException {
        assert signatureParameters != null;
        // Unwrap the signature parameters
        SignatureAppearanceParameters signatureAppearanceParameters = signatureParameters.appearance;
        KeystoreParameters keystoreParameters = signatureParameters.keystore;
        KeyParameters keyParameters = signatureParameters.key;
        String digestAlgorithm = signatureParameters.digestAlgorithm;
        MakeSignature.CryptoStandard sigtype = signatureParameters.sigtype;

        // Initialize the signature appearance
        PdfSignatureAppearance sap = signatureAppearanceParameters.getSignatureAppearance(stp);
        assert sap != null; // `stp` must have been created using `PdfStamper.createSignature` static method

        sign(sap, keystoreParameters, keyParameters, digestAlgorithm, sigtype);
    }

    // Initialize and load the keystore
    private static void sign(PdfSignatureAppearance sap,
            KeystoreParameters keystoreParameters,
            KeyParameters keyParameters,
            String digestAlgorithm,
            MakeSignature.CryptoStandard sigtype) throws OperationException {
        assert keystoreParameters != null;

        // Initialize and load keystore
        KeyStore ks = keystoreParameters.loadKeystore();

        sign(sap, ks, keyParameters, digestAlgorithm, sigtype);
    }

    // Get the private key and the certificate chain from the keystore
    private static void sign(PdfSignatureAppearance sap,
            KeyStore ks,
            KeyParameters keyParameters,
            String digestAlgorithm,
            MakeSignature.CryptoStandard sigtype) throws OperationException {
        assert keyParameters != null;
        // Fix the values, especially if they were not set at all
        keyParameters.fix(ks);

        PrivateKey pk = keyParameters.getPrivateKey(ks);
        Certificate[] chain = keyParameters.getCertificateChain(ks);

        sign(sap, pk, digestAlgorithm, chain, sigtype);
    }

    // Initialize the signature algorithm
    private static void sign(PdfSignatureAppearance sap,
            PrivateKey pk,
            String digestAlgorithm,
            Certificate[] chain,
            MakeSignature.CryptoStandard sigtype) throws OperationException {
        assert digestAlgorithm != null;

        // Initialize the signature algorithm
        logger.info(String.format("Digest algorithm: %s", digestAlgorithm));
        if (DigestAlgorithms.getAllowedDigests(digestAlgorithm) == null) {
            throw new OperationException(String.format("The digest algorithm %s is not supported.", digestAlgorithm));
        }

        logger.info(String.format("Signature security provider: %s", provider.getName()));
        ExternalSignature externalSignature = new PrivateKeySignature(pk, digestAlgorithm, provider.getName());

        sign(sap, externalSignature, chain, sigtype);
    }

    // Set the "external digest" algorithm
    private static void sign(PdfSignatureAppearance sap,
            ExternalSignature externalSignature,
            Certificate[] chain,
            MakeSignature.CryptoStandard sigtype) throws OperationException {
        // Use the static BouncyCastleDigest instance
        sign(sap, OperationSignatureAdd.externalDigest, externalSignature, chain, sigtype);
    }

    // Sign the document
    private static void sign(PdfSignatureAppearance sap,
            ExternalDigest externalDigest,
            ExternalSignature externalSignature,
            Certificate[] chain,
            MakeSignature.CryptoStandard sigtype) throws OperationException {
        // TODO?: Set some of the following parameters more sensibly

        // Certificate Revocation List
        // digitalsignatures20130304.pdf : Section 3.2
        Collection<CrlClient> crlList = null;

        // Online Certificate Status Protocol
        // digitalsignatures20130304.pdf : Section 3.2.4
        OcspClient ocspClient = null;

        // Time Stamp Authority
        // digitalsignatures20130304.pdf : Section 3.3
        TSAClient tsaClient = null;

        // digitalsignatures20130304.pdf : Section 3.5
        // The value of 0 means "try a generous educated guess".
        // We need not change this unless we want to optimize the resulting PDF document size.
        int estimatedSize = 0;

        logger.info(String.format("Cryptographic standard (signature format): %s", sigtype));

        try {
            MakeSignature.signDetached(sap, externalDigest, externalSignature, chain, crlList, ocspClient, tsaClient, estimatedSize, sigtype);
        } catch (IOException | DocumentException | GeneralSecurityException ex) {
            throw new OperationException("Could not sign the document.", ex);
        } catch (NullPointerException ex) {
            throw new OperationException("Could not sign the document. Invalid digest algorithm?", ex);
        }
        logger.info("Document successfully signed.");
    }

}