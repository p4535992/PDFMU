package cz.hobrasoft.pdfmu.operation.signature;

import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import cz.hobrasoft.pdfmu.operation.args.ArgsConfiguration;
import java.util.Calendar;
import java.util.logging.Logger;
import net.sourceforge.argparse4j.inf.ArgumentGroup;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

/**
 *
 * @author <a href="mailto:filip.bartek@hobrasoft.cz">Filip Bartek</a>
 */
class SignatureAppearanceParameters implements ArgsConfiguration {

    public String reason = null;
    public String location = null;
    public String contact = null;
    public Calendar signDate = null;
    public int certificationLevel = PdfSignatureAppearance.NOT_CERTIFIED;

    private static final Logger logger = Logger.getLogger(SignatureAppearanceParameters.class.getName());

    @Override
    public void addArguments(ArgumentParser parser) {
        ArgumentGroup group = parser.addArgumentGroup("signature metadata");
        // TODO: Add description

        // The fields are explained in the following documents:
        // digitalsignatures20130304.pdf : Section 2.3.3
        // http://www.adobe.com/devnet-docs/acrobatetk/tools/DigSig/appearances.html
        // https://www.pdfill.com/document_sign.html
        group.addArgument("--reason")
                .help("What was the reason for signing? (default: <none>)")
                .type(String.class);
        group.addArgument("--location")
                .help("Where was the document signed? (default: <none>)")
                .type(String.class);
        group.addArgument("--contact")
                .help("signer contact information (defualt: <none>)")
                .type(String.class);
        // TODO: Unify the help strings
    }

    @Override
    public void setFromNamespace(Namespace namespace) {
        reason = namespace.getString("reason");
        location = namespace.getString("location");
        contact = namespace.getString("contact");
        // TODO?: Expose `signDate`
        // Note: Argparse4j does not seem to support time
        // (namely {@link Calendar}) natively.
        // TODO?: Expose `certificationLevel`
    }

    public void configureSignatureAppearance(PdfSignatureAppearance sap) {
        assert sap != null;
        // Configure signature metadata
        if (reason != null) {
            logger.info(String.format("Reason: %s", reason));
            sap.setReason(reason);
        }
        if (location != null) {
            logger.info(String.format("Location: %s", location));
            sap.setLocation(location);
        }
        if (contact != null) {
            logger.info(String.format("Contact: %s", contact));
            sap.setContact(contact);
        }
        if (signDate != null) {
            // `setSignDate(null)` crashes
            logger.info(String.format("Date: %s", signDate));
            sap.setSignDate(signDate);
        }
        sap.setCertificationLevel(certificationLevel);

        // TODO?: Set signer's name
        // digitalsignatures20130304.pdf : Code sample 2.12
    }

    public PdfSignatureAppearance getSignatureAppearance(PdfStamper stp) {
        assert stp != null;

        // Initialize the signature appearance
        PdfSignatureAppearance sap = stp.getSignatureAppearance();
        configureSignatureAppearance(sap);

        return sap;
    }

}