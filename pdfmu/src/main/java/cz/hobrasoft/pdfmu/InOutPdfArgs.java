package cz.hobrasoft.pdfmu;

import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;

// Handle unset output file (in-place change)
public class InOutPdfArgs implements ArgsConfiguration, AutoCloseable {

    private final String metavarIn = "IN.pdf";
    public final InPdfArgs in = new InPdfArgs(metavarIn);
    public final OutPdfArgs out = new OutPdfArgs(metavarIn);

    @Override
    public void addArguments(ArgumentParser parser) {
        in.addArguments(parser);
        out.addArguments(parser);
    }

    @Override
    public void setFromNamespace(Namespace namespace) {
        in.setFromNamespace(namespace);
        out.setFromNamespace(namespace);
    }

    public void open() throws OperationException {
        open(false);
    }

    public void openSignature() throws OperationException {
        open(true);
    }

    public void open(boolean signature) throws OperationException {
        open(signature, '\0');
    }

    public void open(char pdfVersion) throws OperationException {
        open(false, pdfVersion);
    }

    public void open(boolean signature, char pdfVersion) throws OperationException {
        PdfReader reader = in.open();
        out.setDefaultFile(in.getFile());
        out.open(reader, signature, pdfVersion);
    }

    public PdfReader getPdfReader() {
        return in.getPdfReader();
    }

    public PdfStamper getPdfStamper() {
        return out.getPdfStamper();
    }

    @Override
    public void close() throws OperationException {
        out.close();
        in.close();
    }

}
