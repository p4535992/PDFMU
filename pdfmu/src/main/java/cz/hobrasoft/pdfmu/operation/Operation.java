package cz.hobrasoft.pdfmu.operation;

import cz.hobrasoft.pdfmu.JsonWriting;
import net.sourceforge.argparse4j.inf.Namespace;
import net.sourceforge.argparse4j.inf.Subparser;

/**
 * Manipulates a PDF file
 *
 * @author <a href="mailto:filip.bartek@hobrasoft.cz">Filip Bartek</a>
 */
public interface Operation extends JsonWriting {

    /**
     * Configure this operation's subparser
     *
     * <p>
     * The subparser handles the arguments accepted by this operation.
     *
     * @param subparser Subparser object to configure, especially adding
     * arguments
     * @return the subparser added to {@code subparsers}
     */
    public Subparser configureSubparser(Subparser subparser);

    /**
     * Executes the operation
     *
     * @param namespace parsed command line arguments
     * @throws OperationException if some condition prevents the operation from
     * finishing
     */
    public void execute(Namespace namespace) throws OperationException;
}
