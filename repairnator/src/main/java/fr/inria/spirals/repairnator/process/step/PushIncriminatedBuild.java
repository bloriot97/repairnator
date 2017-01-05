package fr.inria.spirals.repairnator.process.step;

import fr.inria.spirals.repairnator.Launcher;
import fr.inria.spirals.repairnator.process.ProjectInspector;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.api.RemoteAddCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Created by urli on 05/01/2017.
 */
public class PushIncriminatedBuild extends AbstractStep {
    private static final String REMOTE_REPO = "https://github.com/Spirals-Team/librepair-experiments.git";

    public PushIncriminatedBuild(ProjectInspector inspector) {
        super(inspector);
    }

    @Override
    protected void businessExecute() {
        String branchName = inspector.getRepoSlug().replace('/','-')+'-'+inspector.getBuild().getId();

        Launcher.LOGGER.debug("Start to push failing state in the remote repository: "+REMOTE_REPO+" branch: "+branchName);
        if (System.getenv("GITHUB_OAUTH") == null) {
            Launcher.LOGGER.warn("You must the GITHUB_OAUTH env property to push incriminated build.");
            return;
        }

        try {
            Git git = Git.open(new File(inspector.getRepoLocalPath()));
            RemoteAddCommand remoteAdd = git.remoteAdd();
            remoteAdd.setName("saveFail");
            remoteAdd.setUri(new URIish(REMOTE_REPO));
            remoteAdd.call();

            git.fetch().setRemote("saveFail").call();



            Ref theRef = git.getRepository().findRef("refs/remotes/saveFail/"+branchName);

            if (theRef != null) {
                Launcher.LOGGER.warn("A branch already exist in the remote repo with the following name: "+branchName);
                return;
            }

            Ref branch = git.branchCreate().setName(branchName).call();

            git.push()
                .setRemote("saveFail")
                .add(branch)
                .setCredentialsProvider(new UsernamePasswordCredentialsProvider( System.getenv("GITHUB_OAUTH"), "" ))
                .call();

        } catch (IOException e) {
            Launcher.LOGGER.error("Error while reading git directory at the following location: "+inspector.getRepoLocalPath()+" : "+e);
        } catch (URISyntaxException e) {
            Launcher.LOGGER.error("Error while setting remote repository with following URL: "+REMOTE_REPO+" : "+e);
        } catch (GitAPIException e) {
            Launcher.LOGGER.error("Error while executing a JGit operation: "+e);
        }
    }


}