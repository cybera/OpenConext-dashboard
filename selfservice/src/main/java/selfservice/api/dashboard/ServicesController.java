package selfservice.api.dashboard;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static selfservice.api.dashboard.Constants.HTTP_X_IDP_ENTITY_ID;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.servlet.http.HttpServletResponse;

import com.google.common.base.Strings;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import au.com.bytecode.opencsv.CSVWriter;
import selfservice.domain.Action;
import selfservice.domain.Category;
import selfservice.domain.CoinUser;
import selfservice.domain.InstitutionIdentityProvider;
import selfservice.domain.JiraTask;
import selfservice.domain.Service;
import selfservice.service.Csa;
import selfservice.service.IdentityProviderService;
import selfservice.util.SpringSecurity;

@RestController
@RequestMapping(value = "/dashboard/api/services", produces = APPLICATION_JSON_VALUE)
public class ServicesController extends BaseController {

  private static Set<String> IGNORED_ARP_LABELS = ImmutableSet.of("urn:mace:dir:attribute-def:eduPersonTargetedID");

  @Autowired
  private Csa csa;

  @Autowired
  private IdentityProviderService identityProviderService;

  @RequestMapping
  public RestResponse<List<Service>> index(@RequestHeader(HTTP_X_IDP_ENTITY_ID) String idpEntityId) {
    return createRestResponse(csa.getServicesForIdp(idpEntityId));
  }

  @RequestMapping(value = "/idps")
  public RestResponse<List<InstitutionIdentityProvider>> getConnectedIdps(@RequestHeader(HTTP_X_IDP_ENTITY_ID) String idpEntityId, @RequestParam String spEntityId) {
    List<InstitutionIdentityProvider> idps = identityProviderService.getLinkedIdentityProviders(spEntityId).stream()
        .map(idp -> new InstitutionIdentityProvider(idp.getId(), idp.getName(), idp.getInstitutionId()))
        .collect(toList());

    return createRestResponse(idps);
  }

  @RequestMapping(value = "/download")
  public ResponseEntity<String> download(@RequestParam("idpEntityId") String idpEntityId, @RequestParam("id[]") List<Long> ids, HttpServletResponse response) {
    List<Service> services = csa.getServicesForIdp(idpEntityId);

    List<String[]> rows = Stream.concat(Stream.<String[]>of(new String[] {
        "id", "name", "description", "app-url", "wiki-url", "support-mail",
        "connected", "license", "licenseStatus", "categories", "spEntityId",
        "spName", "publishedInEdugain", "normenkaderPresent", "normenkaderUrl", "singleTenant" }),
        ids.stream()
        .map(id -> getServiceById(services, id))
        .map(service ->
          new String[] {
            String.valueOf(service.getId()),
            service.getName(),
            service.getDescription(),
            service.getAppUrl(),
            service.getWikiUrl(),
            service.getSupportMail(),
            String.valueOf(service.isConnected()),
            service.getLicense() != null ? service.getLicense().toString() : null,
            service.getLicenseStatus().name(),
            service.getCategories().stream().map(Category::getName).collect(joining()),
            service.getSpEntityId(),
            service.getSpName(),
            String.valueOf(service.isPublishedInEdugain()),
            String.valueOf(service.isNormenkaderPresent()),
            service.getNormenkaderUrl(),
            String.valueOf(service.isExampleSingleTenant()) }
        )).collect(toList());

    response.setHeader("Content-Disposition", format("attachment; filename=service-overview.csv"));

    try (CSVWriter writer = new CSVWriter(new OutputStreamWriter(response.getOutputStream()))) {
      writer.writeAll(rows);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }

    return new ResponseEntity<String>(HttpStatus.OK);
  }

  private Service getServiceById(List<Service> services, Long id) {
    return services.stream().filter(service -> service.getId() == id).findFirst().orElse(null);
  }

  @RequestMapping(value = "/id/{id}")
  public RestResponse<Service> get(@RequestHeader(HTTP_X_IDP_ENTITY_ID) String idpEntityId, @PathVariable long id) {
    Service service = csa.getServiceForIdp(idpEntityId, id);

    // remove arp-labels that are explicitly unused
    for (String label : IGNORED_ARP_LABELS) {
      if (service.getArp() != null) {
        service.getArp().getAttributes().remove(label);
      }
    }

    return createRestResponse(service);
  }

  @RequestMapping(value = "/id/{id}/connect", method = RequestMethod.POST)
  public ResponseEntity<Void> connect(@RequestHeader(HTTP_X_IDP_ENTITY_ID) String idpEntityId,
                                              @RequestParam(value = "comments", required = false) String comments,
                                              @RequestParam(value = "spEntityId", required = true) String spEntityId,
                                              @PathVariable String id) {
    if (!createAction(idpEntityId, comments, spEntityId, JiraTask.Type.LINKREQUEST)) {
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);
    }

    return new ResponseEntity<>(HttpStatus.OK);
  }

  @RequestMapping(value = "/id/{id}/disconnect", method = RequestMethod.POST)
  public ResponseEntity<Void> disconnect(@RequestHeader(HTTP_X_IDP_ENTITY_ID) String idpEntityId,
                                                 @RequestParam(value = "comments", required = false) String comments,
                                                 @RequestParam(value = "spEntityId", required = true) String spEntityId,
                                                 @PathVariable String id) {
    if (!createAction(idpEntityId, comments, spEntityId, JiraTask.Type.UNLINKREQUEST))
      return new ResponseEntity<>(HttpStatus.FORBIDDEN);

    return new ResponseEntity<>(HttpStatus.OK);
  }

  private boolean createAction(String idpEntityId, String comments, String spEntityId, JiraTask.Type jiraType) {
    CoinUser currentUser = SpringSecurity.getCurrentUser();
    if (currentUser.isSuperUser() || currentUser.isDashboardViewer()) {
      return false;
    }

    if (Strings.isNullOrEmpty(currentUser.getIdp().getInstitutionId())) {
      return false;
    }

    Action action = new Action();
    action.setUserId(currentUser.getUid());
    action.setUserEmail(currentUser.getEmail());
    action.setUserName(currentUser.getDisplayName());
    action.setType(jiraType);
    action.setBody(comments);
    action.setIdpId(idpEntityId);
    action.setSpId(spEntityId);
    action.setInstitutionId(currentUser.getIdp().getInstitutionId());

    csa.createAction(action);

    return true;
  }
}
