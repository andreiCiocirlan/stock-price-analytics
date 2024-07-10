package stock.price.analytics.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import stock.price.analytics.service.RefreshMaterializedViewsService;

@RestController
@RequestMapping("/refresh")
@RequiredArgsConstructor
public class RefreshMaterializedViewsController {

    private final RefreshMaterializedViewsService refreshMaterializedViewsService;

    @GetMapping("/views")
    public void refreshMaterializedViews() {
        refreshMaterializedViewsService.refreshMaterializedViews();
    }

}
