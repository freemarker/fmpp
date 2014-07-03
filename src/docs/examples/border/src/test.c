#include <string.h>
#include "futil_i.h"

zxz_ec_t
zxz_f_child_path_can_match(zxz_bool_t *pbMatches,
        const char *sPath, const char *sPattern)
{
    zxz_ec_t ec;
    int i;
    char *sNew = NULL;

    i = strlen(sPath);
    if (i != 0 && sPath[i-1] != ZXZ_CFGM_PATH_SEPARATOR_CHR
            && sPath[i-1] != ZXZ_CFGM_PATH_SEPARATOR2_CHR) {
        sNew = malloc(i+2);  /* <-- +sNew  */
        strcpy(sNew, sPath);
        sNew[i] = ZXZ_CFGM_PATH_SEPARATOR_CHR;
        sNew[i+1] = '\0';
        sPath = sNew;
    }
    ec = zxz_f_path_matches_ex(pbMatches, sPath, sPattern, FALSE, 1);
    free(sNew);  /* <-- -sNew  */
    return ec;
}
